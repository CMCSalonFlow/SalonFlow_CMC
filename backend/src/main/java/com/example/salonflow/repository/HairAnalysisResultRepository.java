package com.example.salonflow.repository;

import com.example.salonflow.entity.HairAnalysisResult;
import com.example.salonflow.entity.enums.hair.HairAnalysisStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HairAnalysisResultRepository extends JpaRepository<HairAnalysisResult, Long> {

    List<HairAnalysisResult> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<HairAnalysisResult> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

    List<HairAnalysisResult> findByStatusOrderByCreatedAtAsc(HairAnalysisStatus status);
}
