package com.example.salonflow.services.service;

import com.example.salonflow.dto.Salon.CreateSalonRequest;
import com.example.salonflow.dto.Salon.SalonResponse;

import java.util.List;

public interface SalonService {

    SalonResponse create(CreateSalonRequest request);

    List<SalonResponse> getMySalons();

    SalonResponse getById(Long salonId);
}