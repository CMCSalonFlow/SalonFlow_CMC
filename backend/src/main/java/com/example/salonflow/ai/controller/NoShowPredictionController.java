package com.example.salonflow.ai.controller;

import com.example.salonflow.ai.dto.noshow.*;
import com.example.salonflow.ai.scheduler.NoShowEvaluationScheduler;
import com.example.salonflow.ai.service.NoShowPredictionService;
import com.example.salonflow.entity.NoShowEvaluationLog;
import com.example.salonflow.entity.NoShowModelConfig;
import com.example.salonflow.repository.NoShowEvaluationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ai/no-show")
@RequiredArgsConstructor
@Slf4j
public class NoShowPredictionController {

    private final NoShowPredictionService noShowPredictionService;
    private final NoShowEvaluationScheduler evaluationScheduler;
    private final NoShowEvaluationRepository evaluationRepository;

    @GetMapping("/predict/{bookingId}")
    public ResponseEntity<NoShowPredictionDto> predictNoShow(@PathVariable Long bookingId) {
        NoShowPredictionDto result = noShowPredictionService.getPredictionByBookingId(bookingId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/high-risk")
    public ResponseEntity<Page<NoShowPredictionDto>> getHighRiskBookings(
            @RequestParam Long branchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<NoShowPredictionDto> highRiskPage = noShowPredictionService.getHighRiskBookings(branchId, pageable);
        return ResponseEntity.ok(highRiskPage);
    }

    @GetMapping("/logs")
    public ResponseEntity<Page<NoShowPredictionDto>> getPredictionLogs(
            @RequestParam(required = false) Long branchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<NoShowPredictionDto> logs = noShowPredictionService.getPredictionLogs(branchId, pageable);
        return ResponseEntity.ok(logs);
    }

    @PostMapping("/send-reminder/{bookingId}")
    public ResponseEntity<Boolean> sendReminder(@PathVariable Long bookingId) {
        boolean sent = noShowPredictionService.sendManualReminder(bookingId);
        return ResponseEntity.ok(sent);
    }

    @GetMapping("/config")
    public ResponseEntity<NoShowModelConfig> getModelConfig() {
        NoShowModelConfig config = noShowPredictionService.getModelConfig();
        return ResponseEntity.ok(config);
    }

    @PutMapping("/config")
    public ResponseEntity<NoShowModelConfig> updateModelConfig(@RequestBody UpdateNoShowModelConfigDto dto) {
        NoShowModelConfig updated = noShowPredictionService.updateModelConfig(dto);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/evaluations")
    public ResponseEntity<List<NoShowEvaluationLog>> getEvaluations() {
        List<NoShowEvaluationLog> evaluations = evaluationRepository.findAllByOrderByEvaluationDateDesc();
        return ResponseEntity.ok(evaluations);
    }

    @PostMapping("/evaluations/trigger")
    public ResponseEntity<NoShowEvaluationLog> triggerEvaluation(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(7);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();

        NoShowEvaluationLog logResult = evaluationScheduler.evaluateModelAccuracyForRange(start, end);
        return ResponseEntity.ok(logResult);
    }
}
