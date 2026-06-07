package com.example.salonflow.controller;

import com.example.salonflow.entity.TestEntity;
import com.example.salonflow.repository.TestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/test-supabase")
public class TestController {

    @Autowired
    private TestRepository testRepository;

    @GetMapping("/connect")
    public String testConnection() {
        try {
            TestEntity test = new TestEntity();
            test.setName("Supabase Test");
            test.setStatus("Connected Successfully");
            testRepository.save(test);
            
            List<TestEntity> results = testRepository.findAll();
            return "Connection to Supabase successful! Total records in test_entities: " + results.size();
        } catch (Exception e) {
            return "Connection to Supabase failed: " + e.getMessage();
        }
    }
}