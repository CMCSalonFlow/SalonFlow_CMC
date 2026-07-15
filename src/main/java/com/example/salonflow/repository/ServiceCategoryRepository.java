package com.example.salonflow.repository;

import com.example.salonflow.entity.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, Long> {

    List<ServiceCategory> findAllByOrderByDisplayOrderAsc();

    @Query("SELECT MAX(s.displayOrder) FROM ServiceCategory s")
    Integer findMaxDisplayOrder();
}