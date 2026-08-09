package com.example.salonflow.services.impl;

import com.example.salonflow.dto.forecast.DailyRevenuePoint;
import com.example.salonflow.dto.forecast.ForecastMeta;
import com.example.salonflow.dto.forecast.PythonForecastRequest;
import com.example.salonflow.dto.forecast.PythonForecastResponse;
import com.example.salonflow.dto.forecast.PythonModelStatusResponse;
import com.example.salonflow.dto.forecast.PythonTrainResponse;
import com.example.salonflow.dto.forecast.RevenueForecastModelStatusResponse;
import com.example.salonflow.dto.forecast.RevenueForecastResponse;
import com.example.salonflow.dto.forecast.RevenueForecastTrainResponse;
import com.example.salonflow.exception.ForecastException;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

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

    @Value("${app.forecast.min-data-points:30}")
    private Integer minDataPoints;

    @Override
    @Transactional(readOnly = true)
    public List<DailyRevenuePoint> getDailyRevenueHistory(Long branchId, int months) {
        return getHistoryData(branchId, months).actuals();
    }

    private HistoryData getHistoryData(Long branchId, int months) {
        validateMonths(months);
        ensureBranchExists(branchId);

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(months).plusDays(1);
        List<DailyRevenueProjection> revenueRows = paymentRepository
                .findDailyRevenueByBranch(branchId, startDate, endDate, PaymentStatus.SUCCESS);
        Map<LocalDate, BigDecimal> revenueByDate = revenueRows.stream()
                .collect(Collectors.toMap(
                        DailyRevenueProjection::getDate,
                        DailyRevenueProjection::getRevenue
                ));

        List<DailyRevenuePoint> actuals = startDate
                .datesUntil(endDate.plusDays(1))
                .map(date -> DailyRevenuePoint.builder()
                        .date(date)
                        .revenue(revenueByDate.getOrDefault(date, BigDecimal.ZERO))
                        .build())
                .toList();
        return new HistoryData(actuals, revenueRows.size(), startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public RevenueForecastResponse forecastBranchRevenue(Long branchId, int months, int periods) {
        validatePeriods(periods);
        HistoryData history = getHistoryData(branchId, months);
        validateEnoughData(history);
        String modelKey = modelKey(branchId);

        PythonForecastResponse pythonResponse = callDirectForecast(modelKey, history.actuals(), periods);

        return RevenueForecastResponse.builder()
                .branchId(branchId)
                .modelKey(modelKey)
                .months(months)
                .periods(periods)
                .historyStartDate(history.startDate())
                .historyEndDate(history.endDate())
                .actuals(history.actuals())
                .forecast(pythonResponse.getForecast())
                .meta(buildMeta(months, periods, null, history.dataPoints()))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public RevenueForecastResponse forecastBranchRevenueFromSavedModel(Long branchId, int months, int periods) {
        validatePeriods(periods);
        HistoryData history = getHistoryData(branchId, months);
        String modelKey = modelKey(branchId);

        PythonModelStatusResponse status = getPythonModelStatus(modelKey);
        PythonForecastResponse pythonResponse = callSavedForecast(modelKey, periods);

        return RevenueForecastResponse.builder()
                .branchId(branchId)
                .modelKey(modelKey)
                .months(months)
                .periods(periods)
                .historyStartDate(history.startDate())
                .historyEndDate(history.endDate())
                .actuals(history.actuals())
                .forecast(pythonResponse.getForecast())
                .meta(buildMeta(months, periods, status, history.dataPoints()))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public RevenueForecastModelStatusResponse getBranchRevenueModelStatus(Long branchId) {
        ensureBranchExists(branchId);
        String modelKey = modelKey(branchId);
        try {
            PythonModelStatusResponse status = getPythonModelStatus(modelKey);
            return RevenueForecastModelStatusResponse.builder()
                    .branchId(branchId)
                    .trained(Boolean.TRUE.equals(status.getTrained()))
                    .lastTrainedAt(status.getLastTrainedAt())
                    .trainingMonths(status.getTrainingMonths())
                    .dataPoints(status.getDataPoints())
                    .modelVersion(status.getModelVersion())
                    .build();
        } catch (ForecastException ex) {
            if ("MODEL_NOT_FOUND".equals(ex.getCode())) {
                return RevenueForecastModelStatusResponse.builder()
                        .branchId(branchId)
                        .trained(false)
                        .modelVersion("revenue_prophet_v1")
                        .build();
            }
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public RevenueForecastTrainResponse trainBranchRevenueModel(Long branchId, int months) {
        HistoryData history = getHistoryData(branchId, months);
        validateEnoughData(history);
        String modelKey = modelKey(branchId);

        PythonTrainResponse pythonResponse = callTrain(modelKey, history.actuals(), months);

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

    private PythonForecastResponse callDirectForecast(String modelKey, List<DailyRevenuePoint> history, int periods) {
        try {
            PythonForecastResponse response = forecastClient()
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
            return requireResponse(response, "Forecast service returned empty response");
        } catch (WebClientResponseException ex) {
            throw new ForecastException(HttpStatus.INTERNAL_SERVER_ERROR, "TRAINING_FAILED", "Forecast training failed");
        }
    }

    private PythonForecastResponse callSavedForecast(String modelKey, int periods) {
        try {
            PythonForecastResponse response = forecastClient()
                    .post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/models/revenue/{modelKey}/forecast")
                            .queryParam("periods", periods)
                            .build(modelKey))
                    .retrieve()
                    .bodyToMono(PythonForecastResponse.class)
                    .block(Duration.ofSeconds(timeoutSeconds));
            return requireResponse(response, "Forecast service returned empty saved-model response");
        } catch (WebClientResponseException.NotFound ex) {
            throw new ForecastException(HttpStatus.NOT_FOUND, "MODEL_NOT_FOUND", "Revenue forecast model not found");
        } catch (WebClientResponseException.UnprocessableEntity ex) {
            throw new ForecastException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_PERIODS", "periods must be between 1 and 30");
        } catch (WebClientResponseException ex) {
            throw new ForecastException(HttpStatus.INTERNAL_SERVER_ERROR, "TRAINING_FAILED", "Forecast prediction failed");
        }
    }

    private PythonTrainResponse callTrain(String modelKey, List<DailyRevenuePoint> history, int months) {
        try {
            PythonTrainResponse response = forecastClient()
                    .post()
                    .uri("/models/revenue/train")
                    .bodyValue(PythonForecastRequest.builder()
                            .salonId(modelKey)
                            .history(history)
                            .intervalWidth(intervalWidth)
                            .trainingMonths(months)
                            .build())
                    .retrieve()
                    .bodyToMono(PythonTrainResponse.class)
                    .block(Duration.ofSeconds(timeoutSeconds));
            return requireResponse(response, "Forecast service returned empty train response");
        } catch (WebClientResponseException ex) {
            throw new ForecastException(HttpStatus.INTERNAL_SERVER_ERROR, "TRAINING_FAILED", "Forecast training failed");
        }
    }

    private PythonModelStatusResponse getPythonModelStatus(String modelKey) {
        try {
            PythonModelStatusResponse response = forecastClient()
                    .get()
                    .uri("/models/revenue/{modelKey}/status", modelKey)
                    .retrieve()
                    .bodyToMono(PythonModelStatusResponse.class)
                    .block(Duration.ofSeconds(timeoutSeconds));
            return requireResponse(response, "Forecast service returned empty model status response");
        } catch (WebClientResponseException.NotFound ex) {
            throw new ForecastException(HttpStatus.NOT_FOUND, "MODEL_NOT_FOUND", "Revenue forecast model not found");
        } catch (WebClientResponseException ex) {
            throw new ForecastException(HttpStatus.INTERNAL_SERVER_ERROR, "TRAINING_FAILED", "Forecast model status check failed");
        }
    }

    private <T> T requireResponse(T response, String message) {
        if (response == null) {
            throw new ForecastException(HttpStatus.INTERNAL_SERVER_ERROR, "TRAINING_FAILED", message);
        }
        return response;
    }

    private ForecastMeta buildMeta(
            int months,
            int periods,
            PythonModelStatusResponse status,
            int currentDataPoints
    ) {
        return ForecastMeta.builder()
                .months(months)
                .periods(periods)
                .lastTrainedAt(status != null ? status.getLastTrainedAt() : null)
                .trainingMonths(status != null ? status.getTrainingMonths() : null)
                .dataPoints(status != null && status.getDataPoints() != null ? status.getDataPoints() : currentDataPoints)
                .modelVersion(status != null ? status.getModelVersion() : "revenue_prophet_v1")
                .build();
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
            throw new ForecastException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_PERIODS", "periods must be between 1 and 30");
        }
    }

    private void validateEnoughData(HistoryData history) {
        if (history.dataPoints() < minDataPoints) {
            throw new ForecastException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "INSUFFICIENT_DATA",
                    "Branch does not have enough revenue data for forecasting"
            );
        }
    }

    private record HistoryData(
            List<DailyRevenuePoint> actuals,
            int dataPoints,
            LocalDate startDate,
            LocalDate endDate
    ) {
    }
}
