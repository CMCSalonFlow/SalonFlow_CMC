package com.example.salonflow.services.service;

import com.example.salonflow.dto.forecast.DailyRevenuePoint;
import com.example.salonflow.dto.forecast.RevenueForecastModelStatusResponse;
import com.example.salonflow.dto.forecast.RevenueForecastResponse;
import com.example.salonflow.dto.forecast.RevenueForecastTrainResponse;

import java.util.List;

public interface RevenueForecastService {
    List<DailyRevenuePoint> getDailyRevenueHistory(Long branchId, int months);

    RevenueForecastResponse forecastBranchRevenue(Long branchId, int months, int periods);

    RevenueForecastResponse forecastBranchRevenueFromSavedModel(Long branchId, int months, int periods);

    RevenueForecastModelStatusResponse getBranchRevenueModelStatus(Long branchId);

    RevenueForecastTrainResponse trainBranchRevenueModel(Long branchId, int months);

    void trainAllBranchRevenueModels(int months);
}
