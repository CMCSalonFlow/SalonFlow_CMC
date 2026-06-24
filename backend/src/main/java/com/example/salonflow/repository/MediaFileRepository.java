package com.example.salonflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.salonflow.entity.MediaFile;
public interface MediaFileRepository
        extends JpaRepository<MediaFile, Long> {
}