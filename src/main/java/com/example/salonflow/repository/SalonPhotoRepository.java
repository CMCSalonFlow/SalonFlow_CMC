package com.example.salonflow.repository;

import com.example.salonflow.entity.Salon;
import com.example.salonflow.entity.SalonPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalonPhotoRepository extends JpaRepository<SalonPhoto, Long> {

    List<SalonPhoto> findBySalon(Salon salon);

    void deleteBySalon(Salon salon);

}