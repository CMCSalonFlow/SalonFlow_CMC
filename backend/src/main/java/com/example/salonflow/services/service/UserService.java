package com.example.salonflow.services.service;

import com.example.salonflow.entity.User;

import java.util.List;

public interface UserService {

    User getCurrentUser();

    User getById(Long id);

    User getByEmail(String email);

    List<User> getAllUsers();

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

}