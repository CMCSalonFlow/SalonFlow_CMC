package com.example.salonflow.repository;

import com.example.salonflow.entity.CustomerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerProfileRepository
        extends JpaRepository<CustomerProfile, Long> {

    Optional<CustomerProfile> findByUser_Id(
            Long userId
    );

    Optional<CustomerProfile> findByMembershipCode(
            String membershipCode
    );
}