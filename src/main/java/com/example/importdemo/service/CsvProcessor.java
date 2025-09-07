package com.example.importdemo.service;

import com.opencsv.CSVReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.FileReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class CsvProcessor {
    private static final int BATCH_SIZE = 1000;
    private static final Logger logger = LoggerFactory.getLogger(CsvProcessor.class);

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public CsvProcessor(JdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
    }


    @Transactional
    public int processCsvToStagingTable(Path filePath, String stagingTableName) throws Exception {
        List<Object[]> batchArgs = new ArrayList<>();
        int totalRecordsImported = 0;

        try (CSVReader reader = new CSVReader(new FileReader(filePath.toFile()))) {
            String[] line;
            reader.readNext();

            while ((line = reader.readNext()) != null) {
                batchArgs.add(new Object[]{
                        Long.parseLong(line[0]), // id
                        line[1],                 // first_name
                        line[2],                 // last_name
                        line[3],                 // email
                        line[4],                 // phone
                        Integer.parseInt(line[5]), // age
                        line[6],                 // city
                        line[7]                  // country
                });

                if (batchArgs.size() >= BATCH_SIZE) {
                    saveChunk(batchArgs, stagingTableName);
                    totalRecordsImported += batchArgs.size();
                    batchArgs.clear();
                }
            }

            if (!batchArgs.isEmpty()) {
                saveChunk(batchArgs, stagingTableName);
                totalRecordsImported += batchArgs.size();
            }
        }
        return totalRecordsImported;
    }

    private void saveChunk(List<Object[]> chunk, String stagingTableName) {
        String sql = String.format(
                "INSERT INTO %s (id, first_name, last_name, email, phone, age, city, country) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                stagingTableName
        );

        transactionTemplate.execute(status -> {
            jdbcTemplate.batchUpdate(sql, chunk);
            logger.debug("Saved a chunk of {} records to {}", chunk.size(), stagingTableName);
            return null;
        });
    }

}
