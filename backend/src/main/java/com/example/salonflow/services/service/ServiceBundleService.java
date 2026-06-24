package com.example.salonflow.services.service;

import com.example.salonflow.dto.bundle.CreateBundleRequest;
import com.example.salonflow.dto.bundle.BundleResponse;
import com.example.salonflow.dto.bundle.UpdateBundleRequest;

import java.util.List;

public interface ServiceBundleService {

    BundleResponse create(Long branchId, CreateBundleRequest request);

    List<BundleResponse> getByBranch(Long branchId);

    List<BundleResponse> getByBranchActiveOnly(Long branchId);

    BundleResponse getById(Long branchId, Long bundleId);

    BundleResponse update(Long branchId, Long bundleId, UpdateBundleRequest request);

    void delete(Long branchId, Long bundleId);
}
