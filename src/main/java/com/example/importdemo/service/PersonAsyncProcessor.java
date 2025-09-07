package com.example.importdemo.service;

import com.example.importdemo.configuration.LockProvider;
import com.example.importdemo.repository.PersonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.locks.Lock;

@Component
public class PersonAsyncProcessor {

    private static final Logger logger = LoggerFactory.getLogger(PersonAsyncProcessor.class);
    private static final String IMPORT_LOCK_KEY = "csv-import-lock";
    private final CsvProcessor csvProcessor;
    private final LockProvider lockProvider;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public PersonAsyncProcessor(CsvProcessor csvProcessor, LockProvider lockProvider, JdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
        this.csvProcessor = csvProcessor;
        this.lockProvider = lockProvider;
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
    }


    @Async("taskExecutor")
    public void importAndSwapData(Path filePath) {
        Lock importLock = lockProvider.getLock(IMPORT_LOCK_KEY);

        String timestamp = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
        String stagingTableName = "people_staging_" + timestamp;
        String oldTableName = "people_old_" + timestamp;

        try {
            logger.info("Waiting to acquire lock for import...");
            importLock.lock();
            logger.info("Lock acquired. Starting staging import for file: {}", filePath);
            long startTime = System.currentTimeMillis();

            logger.info("Creating staging table: {}", stagingTableName);
            String createStagingTableSql = String.format(
                    "CREATE TABLE %s (" +
                            "id BIGINT PRIMARY KEY, " +
                            "first_name VARCHAR(255), " +
                            "last_name VARCHAR(255), " +
                            "email VARCHAR(255), " +
                            "phone VARCHAR(50), " +
                            "age INT, " +
                            "city VARCHAR(255), " +
                            "country VARCHAR(255))",
                    stagingTableName
            );
            jdbcTemplate.execute(createStagingTableSql);


            int totalRecords = csvProcessor.processCsvToStagingTable(filePath, stagingTableName);
            logger.info("Successfully loaded {} records to staging table.", totalRecords);


            logger.info("Performing atomic table swap.");
            transactionTemplate.execute(status -> {
                jdbcTemplate.execute("ALTER TABLE people RENAME TO " + oldTableName);
                jdbcTemplate.execute("ALTER TABLE " + stagingTableName + " RENAME TO people");
                return null;
            });
            logger.info("Table swap successful. New data is live.");


            jdbcTemplate.execute("DROP TABLE " + oldTableName);
            logger.info("Old table {} dropped.", oldTableName);

            long endTime = System.currentTimeMillis();
            logger.info("Full 'All or Nothing' import completed in {} ms.", (endTime - startTime));

        } catch (Exception e) {
            logger.error("Critical error during staging import. Rolling back by dropping staging table.", e);
            jdbcTemplate.execute("DROP TABLE IF EXISTS " + stagingTableName);
        } finally {
            importLock.unlock();
            logger.info("Lock released for file: {}", filePath);
            try {
                Files.deleteIfExists(filePath);
                logger.info("Temporary file {} deleted.", filePath);
            } catch (IOException ex) {
                logger.error("Failed to delete temporary file: {}", filePath, ex);
            }
        }
    }


}
