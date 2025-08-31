package com.example.importdemo.controller;

import com.example.importdemo.entity.Person;
import com.example.importdemo.service.PersonImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ImportController {

    @Autowired
    private PersonImportService personImportService;

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

    @GetMapping("/people")
    public ResponseEntity<List<Person>> getAllPeople() {
        List<Person> people = personImportService.getAllPeople();
        return ResponseEntity.ok(people);
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> getCount() {
        Map<String, Object> response = new HashMap<>();
        response.put("count", personImportService.getPersonCount());
        return ResponseEntity.ok(response);
    }
}
