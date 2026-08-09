package com.example.salonflow.controller;

import com.example.salonflow.dto.forecast.DailyRevenuePoint;
import com.example.salonflow.dto.forecast.RevenueForecastResponse;
import com.example.salonflow.dto.forecast.RevenueForecastTrainResponse;
import com.example.salonflow.services.service.RevenueForecastService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/branches/{branchId}/revenue")
@RequiredArgsConstructor
public class RevenueForecastController {

    private final RevenueForecastService revenueForecastService;

    @GetMapping("/history")
    public ResponseEntity<List<DailyRevenuePoint>> getHistory(
            @PathVariable Long branchId,
            @RequestParam(defaultValue = "6") int months
    ) {
        return ResponseEntity.ok(revenueForecastService.getDailyRevenueHistory(branchId, months));
    }

    @GetMapping("/forecast")
    public ResponseEntity<RevenueForecastResponse> forecast(
            @PathVariable Long branchId,
            @RequestParam(defaultValue = "6") int months,
            @RequestParam(defaultValue = "7") int periods
    ) {
        return ResponseEntity.ok(revenueForecastService.forecastBranchRevenue(branchId, months, periods));
    }

    @GetMapping("/forecast/saved")
    public ResponseEntity<RevenueForecastResponse> forecastFromSavedModel(
            @PathVariable Long branchId,
            @RequestParam(defaultValue = "6") int months,
            @RequestParam(defaultValue = "7") int periods
    ) {
        return ResponseEntity.ok(revenueForecastService.forecastBranchRevenueFromSavedModel(branchId, months, periods));
    }

    @PostMapping("/forecast/train")
    public ResponseEntity<RevenueForecastTrainResponse> train(
            @PathVariable Long branchId,
            @RequestParam(defaultValue = "6") int months
    ) {
        return ResponseEntity.ok(revenueForecastService.trainBranchRevenueModel(branchId, months));
    }
}
