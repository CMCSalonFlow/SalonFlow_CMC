package com.example.salonflow.util;


import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordGenerator {

    public static void main(String[] args) {

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String password = "Admin@123";

        String hash = encoder.encode(password);

        System.out.println("Password : " + password);
        System.out.println("BCrypt   : " + hash);
    }
}