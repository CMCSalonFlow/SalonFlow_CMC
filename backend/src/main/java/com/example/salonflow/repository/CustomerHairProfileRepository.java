package com.example.salonflow.repository;

import com.example.salonflow.entity.CustomerHairProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerHairProfileRepository extends JpaRepository<CustomerHairProfile, Long> {

    Optional<CustomerHairProfile> findByUserId(Long userId);
}
