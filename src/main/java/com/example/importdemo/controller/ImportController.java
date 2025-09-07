package com.example.importdemo.controller;

import com.example.importdemo.entity.Person;
import com.example.importdemo.service.PersonImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

@RestController
@RequestMapping("/api")
public class ImportController {

    @Autowired
    private PersonImportService importService;


    @PostMapping("/csv")
    public ResponseEntity<String> importCsv(@RequestParam("file") MultipartFile file) {
        try {
            importService.importCsvAsync(file); // async → może polecieć TaskRejectedException
            return ResponseEntity.accepted().body("Import wystartował (async).");
        } catch (RejectedExecutionException ex) {
            // W tym momencie executor jest zajęty: pool size=1, queue=0 → drugi task odrzucony
            return ResponseEntity.status(409).body("Import już trwa – spróbuj ponownie później.");
        }
    }
/*
    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importCsv(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (file.isEmpty()) {
                response.put("error", "Please select a file to upload");
                return ResponseEntity.badRequest().body(response);
            }

            int totalRecords = personImportService.importPeopleFromCsv(file);
            
            response.put("message", "File imported successfully");
            response.put("totalRecords", totalRecords);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("error", "Failed to import file: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }


 */
}
