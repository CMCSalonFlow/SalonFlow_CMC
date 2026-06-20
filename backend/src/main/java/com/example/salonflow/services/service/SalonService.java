package com.example.salonflow.services.service;

import com.example.salonflow.dto.Salon.CreateSalonRequest;
import com.example.salonflow.dto.Salon.SalonResponse;
import com.example.salonflow.dto.Salon.UpdateSalonRequest;

import java.util.List;

public interface SalonService {

    SalonResponse create(CreateSalonRequest request);

    SalonResponse getMine();

    SalonResponse update(UpdateSalonRequest request);

    void delete();

    List<SalonResponse> getAll();

    SalonResponse getById(Long id);

}