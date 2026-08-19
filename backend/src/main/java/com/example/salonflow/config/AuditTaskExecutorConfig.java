package com.example.salonflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * KHÔNG cần @EnableAsync ở đây — đã được bật toàn cục ở
 * com.example.salonflow.ai.config.AiTaskExecutorConfig.
 * File này chỉ khai báo thêm 1 executor riêng cho audit log
 * để tách biệt khỏi executor của AI, tránh audit bị nghẽn khi
 * AI task đang bận (và ngược lại).
 */
@Configuration
public class AuditTaskExecutorConfig {

    @Bean(name = "auditTaskExecutor")
    public Executor auditTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(6);
        executor.setQueueCapacity(500); // audit log có thể dồn nhiều hơn AI task
        executor.setThreadNamePrefix("audit-log-");
        executor.initialize();
        return executor;
    }
}
