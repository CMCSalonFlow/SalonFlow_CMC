package com.example.salonflow.repository;

import com.example.salonflow.entity.HairStyle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HairStyleRepository extends JpaRepository<HairStyle, Long> {

    Optional<HairStyle> findByCode(String code);

    List<HairStyle> findByIsActiveTrueOrderByPopularityScoreDescSortOrderAscNameAsc();
}
