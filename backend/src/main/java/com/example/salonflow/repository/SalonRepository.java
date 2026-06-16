package com.example.salonflow.repository;

import com.example.salonflow.entity.Salon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SalonRepository
        extends JpaRepository<Salon, Long> {

    List<Salon> findByOwnerId(Long ownerId);

    Optional<Salon> findByIdAndOwnerId(
            Long salonId,
            Long ownerId
    );
}