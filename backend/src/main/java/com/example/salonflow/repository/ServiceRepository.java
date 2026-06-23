package com.example.salonflow.repository;

import com.example.salonflow.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceRepository extends JpaRepository<Service, Long> {

    List<Service> findBySalonId(Long salonId);

    List<Service> findBySalonIdAndIsActiveTrue(Long salonId);

    Optional<Service> findByIdAndSalonId(Long id, Long salonId);

    List<Service> findByCategoryId(Long categoryId);
}