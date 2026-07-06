package com.example.salonflow.services.service;

import com.example.salonflow.dto.cancellation.CancellationPolicyResponse;
import com.example.salonflow.dto.cancellation.UpdateCancellationPolicyRequest;

public interface CancellationPolicyService {

    CancellationPolicyResponse getBySalonId(Long salonId);

    CancellationPolicyResponse updatePolicy(Long salonId, UpdateCancellationPolicyRequest request);

    CancellationPolicyResponse getDefaultPolicy();
}