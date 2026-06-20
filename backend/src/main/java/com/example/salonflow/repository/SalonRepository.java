package com.example.salonflow.repository;

import com.example.salonflow.entity.Salon;
import com.example.salonflow.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SalonRepository extends JpaRepository<Salon, Long> {

    Optional<Salon> findByOwner(User owner);

    boolean existsByOwner(User owner);

}