package com.example.salonflow.repository;

import com.example.salonflow.entity.Salon;
import com.example.salonflow.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface SalonRepository extends JpaRepository<Salon, Long> {

    Optional<Salon> findByOwner(User owner);

    boolean existsByOwner(User owner);

    Optional<Salon> findFirstByOwnerId(Long ownerId);

    Optional<Salon> findByIdAndOwnerId(Long id, Long ownerId);

    @Query("""
            SELECT s FROM Salon s
            LEFT JOIN FETCH s.photos p
            LEFT JOIN FETCH p.media
            WHERE s.id = :id
            """)
    Optional<Salon> findByIdWithFullData(Long id);

    java.util.List<Salon> findByStatus(com.example.salonflow.entity.SalonStatus status);
}
