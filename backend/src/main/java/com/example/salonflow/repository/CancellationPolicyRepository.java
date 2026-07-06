package com.example.salonflow.repository;

import com.example.salonflow.entity.CancellationPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CancellationPolicyRepository extends JpaRepository<CancellationPolicy, Long> {

    Optional<CancellationPolicy> findBySalonId(Long salonId);

    boolean existsBySalonId(Long salonId);
}