package com.example.salonflow.repository;

import com.example.salonflow.entity.NoShowModelConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NoShowModelConfigRepository extends JpaRepository<NoShowModelConfig, Long> {

    Optional<NoShowModelConfig> findFirstByOrderByIdAsc();
}
