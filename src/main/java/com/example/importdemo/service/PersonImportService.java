package com.example.importdemo.service;

import com.example.importdemo.entity.Person;
import com.example.importdemo.repository.PersonRepository;
import com.opencsv.CSVReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class PersonImportService {

    private final PersonRepository personRepository;

    public PersonImportService(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    public int importPeopleFromCsv(MultipartFile file) throws Exception {
        List<Person> people = new ArrayList<>();
        
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            String[] line;
            boolean isFirstLine = true;
            
            while ((line = reader.readNext()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                
                Person person = new Person();
                person.setFirstName(line[1]);
                person.setLastName(line[2]);
                person.setEmail(line[3]);
                person.setPhone(line[4]);
                person.setAge(Integer.parseInt(line[5]));
                person.setCity(line[6]);
                person.setCountry(line[7]);
                
                people.add(person);
            }
        }
        
        personRepository.saveAll(people);
        return people.size();
    }

    public List<Person> getAllPeople() {
        return personRepository.findAll();
    }

    public long getPersonCount() {
        return personRepository.count();
    }
}
