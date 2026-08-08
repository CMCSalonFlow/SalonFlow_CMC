package com.example.salonflow.ai.scheduler;

import com.example.salonflow.entity.Booking;
import com.example.salonflow.entity.NoShowEvaluationLog;
import com.example.salonflow.entity.NoShowPredictionLog;
import com.example.salonflow.entity.enums.BookingStatus;
import com.example.salonflow.repository.BookingRepository;
import com.example.salonflow.repository.NoShowEvaluationRepository;
import com.example.salonflow.repository.NoShowPredictionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NoShowEvaluationScheduler {

    private final NoShowPredictionRepository predictionRepository;
    private final NoShowEvaluationRepository evaluationRepository;
    private final BookingRepository bookingRepository;

    /**
     * Tự động chạy định kỳ hàng tuần vào lúc 00:00 sáng Chủ Nhật.
     */
    @Scheduled(cron = "0 0 0 * * SUN")
    @Transactional
    public void evaluateWeeklyModelAccuracy() {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today;
        LocalDate startDate = today.minusDays(7);

        evaluateModelAccuracyForRange(startDate, endDate);
    }

    /**
     * Hàm tính toán đánh giá Accuracy cho một khoảng ngày cụ thể (có thể gọi trực tiếp từ API test).
     */
    @Transactional
    public NoShowEvaluationLog evaluateModelAccuracyForRange(LocalDate startDate, LocalDate endDate) {
        log.info("Bắt đầu đánh giá định kỳ Accuracy mô hình AI No-Show từ ngày {} đến {}", startDate, endDate);

        Instant startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endInstant = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        List<NoShowPredictionLog> logs = predictionRepository.findLogsBetweenDates(startInstant, endInstant);

        int tp = 0, fp = 0, tn = 0, fn = 0;
        int totalEvaluated = 0;

        for (NoShowPredictionLog pLog : logs) {
            Booking booking = bookingRepository.findById(pLog.getBookingId()).orElse(null);
            if (booking == null) continue;

            BookingStatus status = booking.getStatus();
            if (status != BookingStatus.COMPLETED && status != BookingStatus.CANCELLED && status != BookingStatus.NO_SHOW) {
                continue; // Bỏ qua các booking chưa kết thúc ca
            }

            totalEvaluated++;
            boolean actualNoShow = (status == BookingStatus.CANCELLED || status == BookingStatus.NO_SHOW);
            boolean predictedNoShow = (pLog.getProbability() != null && pLog.getProbability() >= 0.70) || "HIGH".equalsIgnoreCase(pLog.getRiskLevel());

            if (predictedNoShow && actualNoShow) {
                tp++;
            } else if (predictedNoShow && !actualNoShow) {
                fp++;
            } else if (!predictedNoShow && !actualNoShow) {
                tn++;
            } else {
                fn++;
            }
        }

        double accuracy = totalEvaluated > 0 ? (double) (tp + tn) / totalEvaluated : 1.0;
        double precision = (tp + fp) > 0 ? (double) tp / (tp + fp) : 1.0;
        double recall = (tp + fn) > 0 ? (double) tp / (tp + fn) : 1.0;
        double f1Score = (precision + recall) > 0 ? 2.0 * (precision * recall) / (precision + recall) : 1.0;

        NoShowEvaluationLog evalLog = NoShowEvaluationLog.builder()
                .evaluationDate(LocalDate.now())
                .startDate(startDate)
                .endDate(endDate)
                .totalEvaluatedBookings(totalEvaluated)
                .truePositives(tp)
                .falsePositives(fp)
                .trueNegatives(tn)
                .falseNegatives(fn)
                .accuracy(Math.round(accuracy * 1000.0) / 1000.0)
                .precisionScore(Math.round(precision * 1000.0) / 1000.0)
                .recallScore(Math.round(recall * 1000.0) / 1000.0)
                .f1Score(Math.round(f1Score * 1000.0) / 1000.0)
                .notes(String.format("Đánh giá tự động %d booking: Accuracy=%.1f%%, Precision=%.1f%%, Recall=%.1f%%, F1=%.1f%%",
                        totalEvaluated, accuracy * 100, precision * 100, recall * 100, f1Score * 100))
                .build();

        NoShowEvaluationLog saved = evaluationRepository.save(evalLog);
        log.info("Hoàn tất đánh giá Accuracy AI No-Show: Total={}, Accuracy={}%, F1={}%",
                totalEvaluated, Math.round(accuracy * 100), Math.round(f1Score * 100));

        return saved;
    }
}
