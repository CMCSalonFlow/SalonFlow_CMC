package com.example.salonflow.services.service;

import com.example.salonflow.dto.Salon.CreateSalonRequest;
import com.example.salonflow.dto.Salon.SalonResponse;
import com.example.salonflow.dto.Salon.UpdateSalonRequest;
import com.example.salonflow.entity.Salon;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;

import com.example.salonflow.dto.Salon.RejectSalonRequest;
import com.example.salonflow.dto.Salon.SalonApprovalAuditResponse;
import com.example.salonflow.entity.SalonStatus;

public interface SalonService {

    SalonResponse create(CreateSalonRequest request);

    SalonResponse getMine();

    SalonResponse update(UpdateSalonRequest request);

    void delete();

    List<SalonResponse> getAll();

    SalonResponse getById(Long id);

    List<SalonResponse> getByStatus(SalonStatus status);

    SalonResponse approve(Long salonId, Long adminUserId);

    SalonResponse reject(Long salonId, RejectSalonRequest request, Long adminUserId);

    SalonResponse appeal(Long salonId);

    List<SalonApprovalAuditResponse> getAudits(Long salonId);

    List<SalonResponse> getPublicSalons();
}