package com.example.salonflow.repository;

import com.example.salonflow.entity.HairStyleImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HairStyleImageRepository extends JpaRepository<HairStyleImage, Long> {

    List<HairStyleImage> findByHairStyleIdAndIsActiveTrueOrderByIsCoverDescDisplayOrderAscIdAsc(Long hairStyleId);

    Optional<HairStyleImage> findFirstByHairStyleIdAndIsCoverTrueAndIsActiveTrue(Long hairStyleId);

    Optional<HairStyleImage> findByHairStyleIdAndMediaId(Long hairStyleId, Long mediaId);
}
