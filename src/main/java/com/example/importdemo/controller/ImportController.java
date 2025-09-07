package com.example.importdemo.controller;

import com.example.importdemo.entity.ImportInfoDto;
import com.example.importdemo.entity.Person;
import com.example.importdemo.service.PersonImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ImportController {


    private final PersonImportService personImportService;

    public ImportController(PersonImportService personImportService) {
        this.personImportService = personImportService;
    }

    @PostMapping("/import")
    public ResponseEntity<ImportInfoDto> importCsv(@RequestParam("file") MultipartFile file) {
        ImportInfoDto info = personImportService.startCsvImport(file);
        return ResponseEntity.accepted().body(info);
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
