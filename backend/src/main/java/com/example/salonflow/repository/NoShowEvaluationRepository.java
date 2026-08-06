package com.example.salonflow.repository;

import com.example.salonflow.entity.NoShowEvaluationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoShowEvaluationRepository extends JpaRepository<NoShowEvaluationLog, Long> {

    List<NoShowEvaluationLog> findAllByOrderByEvaluationDateDesc();

    Page<NoShowEvaluationLog> findAllByOrderByEvaluationDateDesc(Pageable pageable);
}
