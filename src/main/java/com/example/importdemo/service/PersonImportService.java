package com.example.importdemo.service;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class PersonImportService {

    private final JdbcTemplate jdbcTemplate;
    private final MeterRegistry meterRegistry;

    private static final Semaphore SEM = new Semaphore(1);

    @Value("${app.csv.batch-size:4000}")
    private int batchSize;

    private final AtomicLong lastRows = new AtomicLong(0);
    private final AtomicReference<Double> lastSeconds = new AtomicReference<>(0.0);
    private final AtomicReference<Double> lastRps = new AtomicReference<>(0.0);

    public PersonImportService(JdbcTemplate jdbcTemplate, MeterRegistry meterRegistry) {
        this.jdbcTemplate = jdbcTemplate;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    void gauges() {
        Gauge.builder("import.last.rows", lastRows, AtomicLong::get).register(meterRegistry);
        Gauge.builder("import.last.seconds", lastSeconds, AtomicReference::get).register(meterRegistry);
        Gauge.builder("import.last.rps", lastRps, AtomicReference::get).register(meterRegistry);
    }


    @Async
    @Transactional
    public void importCsvAsync(MultipartFile file) {
        if (!SEM.tryAcquire()) throw new IllegalStateException("Import is processed");

        long start = System.nanoTime();
        AtomicLong processed = new AtomicLong(0);

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8), 64 * 1024)) {

            jdbcTemplate.execute((ConnectionCallback<Void>) conn -> {
                conn.setAutoCommit(false);
                conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

                final String sql = "INSERT INTO person (first_name, last_name, email, phone, age, city, country) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)";

                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    String line;
                    int inBatch = 0;
                    int lineNo = 0;

                    while ((line = br.readLine()) != null) {
                        lineNo++;
                        if (line.isBlank()) continue;

                        String[] p = line.split(",", -1);

                        if (lineNo == 1 && p[0] != null && !p[0].isEmpty() && p[0].charAt(0) == '\uFEFF') {
                            p[0] = p[0].substring(1);
                        }

                        final boolean header = whatObject(p);
                        final int len = p.length;

                        final int offset;

                        if (header) {
                            continue;
                        } else if (len == 8) {
                            offset = 1;
                        } else if (len == 7) {
                            offset = 0;
                        } else {
                            throw new IllegalArgumentException("Bad cols at line " + lineNo + ": " + len + " -> " + line);
                        }

                        String firstName = p[0 + offset].trim();
                        String lastName = p[1 + offset].trim();
                        String email = p[2 + offset].trim();
                        String phone = p[3 + offset].trim();
                        Integer age = safeParseInt(p[4 + offset]);
                        String city = p[5 + offset].trim();
                        String country = p[6 + offset].trim();

                        ps.setString(1, firstName);
                        ps.setString(2, lastName);
                        ps.setString(3, email);
                        ps.setString(4, phone);
                        if (age == null) ps.setNull(5, Types.INTEGER);
                        else ps.setInt(5, age);
                        ps.setString(6, city);
                        ps.setString(7, country);

                        ps.addBatch();
                        inBatch++;
                        processed.incrementAndGet();

                        if (inBatch >= batchSize) {
                            ps.executeBatch();
                            inBatch = 0;
                        }
                    }
                    if (inBatch > 0) ps.executeBatch();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return null;
            });

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            SEM.release();

            double seconds = (System.nanoTime() - start) / 1_000_000_000.0;

            double rps = seconds > 0 ? processed.get() / seconds : 0.0;

            lastRows.set(processed.get());
            lastSeconds.set(seconds);
            lastRps.set(rps);

            System.out.printf("IMPORT: %d rows in %.3f s (%.0f rows/s)%n", processed.get(), seconds, rps);
        }
    }

    private static boolean whatObject(String[] p) {
        if (p.length >= 6 && (!isInteger(p[0]) || !isInteger(p[5]))) {
            return true;
        } else {
            return false;
        }
    }

    private static boolean isInteger(String s) {
        if (s == null) {
            return false;
        }
        try {
            Integer.parseInt(s.trim());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static Integer safeParseInt(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}