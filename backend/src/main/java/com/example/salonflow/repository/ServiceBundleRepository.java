package com.example.salonflow.repository;

import com.example.salonflow.entity.ServiceBundle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceBundleRepository extends JpaRepository<ServiceBundle, Long> {

    List<ServiceBundle> findBySalonId(Long salonId);

    List<ServiceBundle> findBySalonIdAndIsActiveTrue(Long salonId);

    Optional<ServiceBundle> findByIdAndSalonId(Long id, Long salonId);
}
