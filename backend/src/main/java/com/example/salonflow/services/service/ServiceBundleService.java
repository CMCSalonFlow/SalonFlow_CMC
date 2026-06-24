package com.example.salonflow.services.service;

import com.example.salonflow.dto.bundle.CreateBundleRequest;
import com.example.salonflow.dto.bundle.BundleResponse;
import com.example.salonflow.dto.bundle.UpdateBundleRequest;

import java.util.List;

public interface ServiceBundleService {

    BundleResponse create(Long salonId, CreateBundleRequest request);

    List<BundleResponse> getBySalon(Long salonId);

    List<BundleResponse> getBySalonActiveOnly(Long salonId);

    BundleResponse getById(Long salonId, Long bundleId);

    BundleResponse update(Long salonId, Long bundleId, UpdateBundleRequest request);

    void delete(Long salonId, Long bundleId);
}
