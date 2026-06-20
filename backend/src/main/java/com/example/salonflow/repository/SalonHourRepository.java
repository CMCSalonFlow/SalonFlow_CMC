package com.example.salonflow.repository;

import com.example.salonflow.entity.Salon;
import com.example.salonflow.entity.SalonHour;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalonHourRepository extends JpaRepository<SalonHour, Long> {

    List<SalonHour> findBySalon(Salon salon);

    void deleteBySalon(Salon salon);

}