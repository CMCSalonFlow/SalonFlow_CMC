package com.example.salonflow.controller;

import com.example.salonflow.entity.User;
import com.example.salonflow.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/test-supabase")
public class TestController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/connect")
    public String testConnection() {
        try {
            List<User> users = userRepository.findAll();
            StringBuilder sb = new StringBuilder("Connection to database successful!\n");
            sb.append("Total users in database: ").append(users.size()).append("\n");
            for (User user : users) {
                sb.append("- Username: ").append(user.getUsername())
                  .append(", Email: ").append(user.getEmail())
                  .append(", Full Name: ").append(user.getFullName())
                  .append(", Status: ").append(user.getStatus())
                  .append(", Roles: ");
                user.getRoles().forEach(role -> sb.append(role.getName()).append(" "));
                sb.append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "Connection to database failed: " + e.getMessage();
        }
    }
}