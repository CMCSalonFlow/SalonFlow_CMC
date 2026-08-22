package com.example.salonflow.scheduler;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
public class AuditLogMaintenanceScheduler {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Tạo trước partition tháng sau. Chạy ngày 25 hàng tháng, 02:00.
     */
    @Scheduled(cron = "0 0 2 25 * *")
    @Transactional
    public void createNextMonthPartition() {
        log.info("Creating next month audit_logs partition...");
        entityManager.createNativeQuery(
                "SELECT create_audit_log_partition((date_trunc('month', now()) + interval '1 month')::date)"
        ).getSingleResult();
    }

    /**
     * Dọn các partition quá 2 năm (GDPR retention). Chạy ngày 1 hàng tháng, 03:00.
     */
    @Scheduled(cron = "0 0 3 1 * *")
    @Transactional
    public void cleanupOldPartitions() {
        log.info("Cleaning up audit_logs partitions older than 24 months...");
        entityManager.createNativeQuery("SELECT cleanup_old_audit_partitions()").getSingleResult();
    }
}
