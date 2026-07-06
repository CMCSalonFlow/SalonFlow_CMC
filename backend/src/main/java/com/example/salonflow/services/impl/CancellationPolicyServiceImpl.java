package com.example.salonflow.services.impl;

import com.example.salonflow.dto.cancellation.CancellationPolicyResponse;
import com.example.salonflow.dto.cancellation.UpdateCancellationPolicyRequest;
import com.example.salonflow.entity.CancellationPolicy;
import com.example.salonflow.entity.Salon;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.repository.CancellationPolicyRepository;
import com.example.salonflow.repository.SalonRepository;
import com.example.salonflow.services.service.CancellationPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional
public class CancellationPolicyServiceImpl implements CancellationPolicyService {

    private final CancellationPolicyRepository policyRepository;
    private final SalonRepository salonRepository;

    @Override
    public CancellationPolicyResponse getBySalonId(Long salonId) {
        CancellationPolicy policy = policyRepository.findBySalonId(salonId)
                .orElseGet(() -> createDefaultPolicy(salonId));

        return convertToResponse(policy);
    }

    @Override
    public CancellationPolicyResponse updatePolicy(Long salonId, UpdateCancellationPolicyRequest request) {
        CancellationPolicy policy = policyRepository.findBySalonId(salonId)
                .orElseGet(() -> createDefaultPolicy(salonId));

        if (request.getFreeCancelHours() != null) {
            policy.setFreeCancelHours(request.getFreeCancelHours());
        }
        if (request.getFeePercentage() != null) {
            policy.setFeePercentage(request.getFeePercentage());
        }
        if (request.getIsActive() != null) {
            policy.setIsActive(request.getIsActive());
        }

        CancellationPolicy saved = policyRepository.save(policy);
        return convertToResponse(saved);
    }

    private CancellationPolicy createDefaultPolicy(Long salonId) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy salon"));

        CancellationPolicy policy = CancellationPolicy.builder()
                .salon(salon)
                .freeCancelHours(24)
                .feePercentage(BigDecimal.valueOf(10.0))
                .isActive(true)
                .build();

        return policyRepository.save(policy);
    }

    private CancellationPolicyResponse convertToResponse(CancellationPolicy policy) {
        return CancellationPolicyResponse.builder()
                .id(policy.getId())
                .salonId(policy.getSalon().getId())
                .freeCancelHours(policy.getFreeCancelHours())
                .feePercentage(policy.getFeePercentage())
                .isActive(policy.getIsActive())
                .build();
    }

    @Override
    public CancellationPolicyResponse getDefaultPolicy() {
        return CancellationPolicyResponse.builder()
                .freeCancelHours(24)
                .feePercentage(BigDecimal.valueOf(10.0))
                .isActive(true)
                .build();
    }
}