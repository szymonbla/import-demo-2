package com.example.importdemo.service;

import com.example.importdemo.entity.ImportInfoDto;
import com.example.importdemo.entity.Person;
import com.example.importdemo.repository.PersonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Service
public class PersonImportService {

    private final PersonRepository personRepository;
    private static final Logger logger = LoggerFactory.getLogger(PersonImportService.class);

    private final PersonAsyncProcessor personAsyncProcessor;

    public PersonImportService(PersonRepository personRepository, PersonAsyncProcessor personAsyncProcessor) {
        this.personRepository = personRepository;
        this.personAsyncProcessor = personAsyncProcessor;
    }

    @Transactional
    public ImportInfoDto startCsvImport(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty.");
        }

        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("import-", ".csv");
            Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);
            logger.info("File saved temporarily to: {}", tempFile);

            personAsyncProcessor.importAndSwapData(tempFile);

            return new ImportInfoDto("File import started in the background.");

        } catch (IOException e) {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                }
            }
            logger.error("Failed to store temporary file", e);
            throw new RuntimeException("Failed to process uploaded file.", e);
        }
    }


    public List<Person> getAllPeople() {
        return personRepository.findAll();
    }

    public long getPersonCount() {
        return personRepository.count();
    }
}
