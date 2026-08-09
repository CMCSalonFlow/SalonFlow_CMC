package com.example.salonflow.services.impl;

import com.example.salonflow.dto.forecast.DailyRevenuePoint;
import com.example.salonflow.dto.forecast.PythonForecastRequest;
import com.example.salonflow.dto.forecast.PythonForecastResponse;
import com.example.salonflow.dto.forecast.PythonTrainResponse;
import com.example.salonflow.dto.forecast.RevenueForecastResponse;
import com.example.salonflow.dto.forecast.RevenueForecastTrainResponse;
import com.example.salonflow.entity.Branch;
import com.example.salonflow.entity.enums.PaymentStatus;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.repository.BranchRepository;
import com.example.salonflow.repository.DailyRevenueProjection;
import com.example.salonflow.repository.PaymentRepository;
import com.example.salonflow.services.service.RevenueForecastService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RevenueForecastServiceImpl implements RevenueForecastService {

    private final PaymentRepository paymentRepository;
    private final BranchRepository branchRepository;
    private final WebClient.Builder webClientBuilder;

    @Value("${app.forecast.base-url:http://localhost:8001}")
    private String forecastBaseUrl;

    @Value("${app.forecast.interval-width:0.8}")
    private Double intervalWidth;

    @Value("${app.forecast.timeout-seconds:60}")
    private Long timeoutSeconds;

    @Override
    @Transactional(readOnly = true)
    public List<DailyRevenuePoint> getDailyRevenueHistory(Long branchId, int months) {
        validateMonths(months);
        ensureBranchExists(branchId);

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(months).plusDays(1);
        Map<LocalDate, BigDecimal> revenueByDate = paymentRepository
                .findDailyRevenueByBranch(branchId, startDate, endDate, PaymentStatus.SUCCESS)
                .stream()
                .collect(Collectors.toMap(
                        DailyRevenueProjection::getDate,
                        DailyRevenueProjection::getRevenue
                ));

        return startDate
                .datesUntil(endDate.plusDays(1))
                .map(date -> DailyRevenuePoint.builder()
                        .date(date)
                        .revenue(revenueByDate.getOrDefault(date, BigDecimal.ZERO))
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RevenueForecastResponse forecastBranchRevenue(Long branchId, int months, int periods) {
        validatePeriods(periods);
        List<DailyRevenuePoint> history = getDailyRevenueHistory(branchId, months);
        String modelKey = modelKey(branchId);

        PythonForecastResponse pythonResponse = forecastClient()
                .post()
                .uri("/forecast/revenue")
                .bodyValue(PythonForecastRequest.builder()
                        .salonId(modelKey)
                        .history(history)
                        .periods(periods)
                        .intervalWidth(intervalWidth)
                        .build())
                .retrieve()
                .bodyToMono(PythonForecastResponse.class)
                .block(Duration.ofSeconds(timeoutSeconds));

        if (pythonResponse == null) {
            throw new IllegalStateException("Forecast service returned empty response");
        }

        return RevenueForecastResponse.builder()
                .branchId(branchId)
                .modelKey(modelKey)
                .months(months)
                .periods(periods)
                .historyStartDate(history.getFirst().getDate())
                .historyEndDate(history.getLast().getDate())
                .actuals(history)
                .forecast(pythonResponse.getForecast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public RevenueForecastResponse forecastBranchRevenueFromSavedModel(Long branchId, int months, int periods) {
        validatePeriods(periods);
        List<DailyRevenuePoint> history = getDailyRevenueHistory(branchId, months);
        String modelKey = modelKey(branchId);

        PythonForecastResponse pythonResponse = forecastClient()
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/models/revenue/{modelKey}/forecast")
                        .queryParam("periods", periods)
                        .build(modelKey))
                .retrieve()
                .bodyToMono(PythonForecastResponse.class)
                .block(Duration.ofSeconds(timeoutSeconds));

        if (pythonResponse == null) {
            throw new IllegalStateException("Forecast service returned empty saved-model response");
        }

        return RevenueForecastResponse.builder()
                .branchId(branchId)
                .modelKey(modelKey)
                .months(months)
                .periods(periods)
                .historyStartDate(history.getFirst().getDate())
                .historyEndDate(history.getLast().getDate())
                .actuals(history)
                .forecast(pythonResponse.getForecast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public RevenueForecastTrainResponse trainBranchRevenueModel(Long branchId, int months) {
        List<DailyRevenuePoint> history = getDailyRevenueHistory(branchId, months);
        String modelKey = modelKey(branchId);

        PythonTrainResponse pythonResponse = forecastClient()
                .post()
                .uri("/models/revenue/train")
                .bodyValue(PythonForecastRequest.builder()
                        .salonId(modelKey)
                        .history(history)
                        .intervalWidth(intervalWidth)
                        .build())
                .retrieve()
                .bodyToMono(PythonTrainResponse.class)
                .block(Duration.ofSeconds(timeoutSeconds));

        if (pythonResponse == null) {
            throw new IllegalStateException("Forecast service returned empty train response");
        }

        return RevenueForecastTrainResponse.builder()
                .branchId(branchId)
                .modelKey(modelKey)
                .modelPath(pythonResponse.getModelPath())
                .trainedPoints(pythonResponse.getTrainedPoints())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public void trainAllBranchRevenueModels(int months) {
        List<Branch> branches = branchRepository.findAll();
        for (Branch branch : branches) {
            try {
                trainBranchRevenueModel(branch.getId(), months);
                log.info("Trained revenue forecast model for branch {}", branch.getId());
            } catch (Exception ex) {
                log.warn("Failed to train revenue forecast model for branch {}", branch.getId(), ex);
            }
        }
    }

    private WebClient forecastClient() {
        return webClientBuilder.clone().baseUrl(forecastBaseUrl).build();
    }

    private void ensureBranchExists(Long branchId) {
        if (!branchRepository.existsById(branchId)) {
            throw new ResourceNotFoundException("Khong tim thay chi nhanh voi ID: " + branchId);
        }
    }

    private String modelKey(Long branchId) {
        return "branch-" + branchId;
    }

    private void validateMonths(int months) {
        if (months < 1 || months > 24) {
            throw new IllegalArgumentException("months must be between 1 and 24");
        }
    }

    private void validatePeriods(int periods) {
        if (periods < 1 || periods > 30) {
            throw new IllegalArgumentException("periods must be between 1 and 30");
        }
    }
}
