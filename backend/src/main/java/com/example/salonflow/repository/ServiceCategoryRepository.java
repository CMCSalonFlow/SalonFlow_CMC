package com.example.salonflow.repository;

import com.example.salonflow.entity.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, Long> {

    List<ServiceCategory> findAllByOrderByDisplayOrderAsc();

    List<ServiceCategory> findBySalonIdOrderByDisplayOrderAsc(Long salonId);

    @Query("SELECT MAX(s.displayOrder) FROM ServiceCategory s")
    Integer findMaxDisplayOrder();

    @Query("SELECT MAX(s.displayOrder) FROM ServiceCategory s WHERE s.salonId = :salonId")
    Integer findMaxDisplayOrderBySalonId(Long salonId);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsByNameIgnoreCaseAndSalonId(String name, Long salonId);

    boolean existsByNameIgnoreCaseAndSalonIdAndIdNot(String name, Long salonId, Long id);
}