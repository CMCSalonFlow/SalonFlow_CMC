package com.example.salonflow.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.salonflow.security.SecurityUtils;
import com.example.salonflow.services.service.BranchAccessService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.example.salonflow.services.service.SmsService;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final BranchAccessService branchAccessService;
    private final SmsService smsService;

    @GetMapping("/context")
    public Map<String, Object> context() {

        branchAccessService
                .validateCurrentBranchAccess();

        return Map.of(
                "userId",
                SecurityUtils.getCurrentUserId(),
                "branchId",
                SecurityUtils.getCurrentBranchId()
        );
    }

    @PostMapping("/send-sms")
    public ResponseEntity<Map<String, Object>> testSendSms(
            @RequestParam String phone,
            @RequestParam(required = false, defaultValue = "SalonFlow: Kính chào quý khách, đây là tin nhắn thử nghiệm gửi SMS!") String message
    ) {
        boolean success = smsService.sendSms(phone, message);
        return ResponseEntity.ok(Map.of(
                "success", success,
                "phone", phone,
                "message", message
        ));
    }
}
