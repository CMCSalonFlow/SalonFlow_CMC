package com.example.salonflow.repository;

import com.example.salonflow.entity.Salon;
import com.example.salonflow.entity.SalonHour;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalonHourRepository extends JpaRepository<SalonHour, Long> {

    List<SalonHour> findBySalon(Salon salon);

    java.util.Optional<SalonHour> findBySalonIdAndDayOfWeek(Long salonId, Integer dayOfWeek);

    void deleteBySalon(Salon salon);

}