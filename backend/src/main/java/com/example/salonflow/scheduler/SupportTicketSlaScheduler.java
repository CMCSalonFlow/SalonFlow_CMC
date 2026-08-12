package com.example.salonflow.scheduler;

import com.example.salonflow.services.service.SupportTicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SupportTicketSlaScheduler {

    private final SupportTicketService supportTicketService;

    /**
     * Tự động quét và đánh dấu các Support Ticket quá hạn SLA mỗi 15 phút
     */
    @Scheduled(cron = "0 */15 * * * *")
    public void scanTicketSlaBreaches() {
        log.info("=== Bắt đầu thực thi Scheduled Job: Quét vi phạm SLA Support Ticket ===");
        try {
            supportTicketService.scanAndMarkSlaBreaches();
        } catch (Exception e) {
            log.error("Lỗi khi chạy Scheduled Job quét vi phạm SLA Ticket: {}", e.getMessage(), e);
        }
    }
}
