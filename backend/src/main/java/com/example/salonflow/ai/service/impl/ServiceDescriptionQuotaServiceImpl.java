package com.example.salonflow.ai.service.impl;

import com.example.salonflow.ai.config.AiProperties;
import com.example.salonflow.ai.dto.description.ServiceDescriptionQuotaResponse;
import com.example.salonflow.ai.service.ServiceDescriptionQuotaService;
import com.example.salonflow.exception.BadRequestException;
import com.example.salonflow.exception.TooManyRequestsException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceDescriptionQuotaServiceImpl implements ServiceDescriptionQuotaService {

    private static final ZoneId QUOTA_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String KEY_PREFIX = "ai:service-description:quota";

    private final StringRedisTemplate redisTemplate;
    private final AiProperties aiProperties;

    @Override
    public ServiceDescriptionQuotaResponse getQuota(Long salonId) {
        validateSalonId(salonId);

        int limit = resolveDailyLimit();
        LocalDate quotaDate = LocalDate.now(QUOTA_ZONE);
        String key = buildKey(salonId, quotaDate);

        Integer usedToday = readUsage(key);
        int remainingToday = Math.max(0, limit - usedToday);

        return new ServiceDescriptionQuotaResponse(
                salonId,
                quotaDate,
                usedToday,
                limit,
                remainingToday,
                nextResetAt(quotaDate)
        );
    }

    @Override
    public ServiceDescriptionQuotaResponse consumeQuota(Long salonId) {
        validateSalonId(salonId);

        int limit = resolveDailyLimit();
        LocalDate quotaDate = LocalDate.now(QUOTA_ZONE);
        String key = buildKey(salonId, quotaDate);
        long ttlSeconds = secondsUntilNextReset(quotaDate);

        Long result = redisTemplate.execute(
                consumeScript(),
                List.of(key),
                String.valueOf(limit),
                String.valueOf(ttlSeconds)
        );

        if (result == null) {
            throw new BadRequestException("Unable to consume service description quota");
        }
        if (result < 0) {
            throw new TooManyRequestsException("Daily AI quota reached for this salon");
        }

        int usedToday = result.intValue();
        int remainingToday = Math.max(0, limit - usedToday);

        return new ServiceDescriptionQuotaResponse(
                salonId,
                quotaDate,
                usedToday,
                limit,
                remainingToday,
                nextResetAt(quotaDate)
        );
    }

    private void validateSalonId(Long salonId) {
        if (salonId == null) {
            throw new BadRequestException("Salon id is required");
        }
    }

    private int resolveDailyLimit() {
        Integer configured = aiProperties.getServiceDescription() != null
                ? aiProperties.getServiceDescription().getDailyQuotaPerSalon()
                : null;
        if (configured == null || configured <= 0) {
            return 10;
        }
        return configured;
    }

    private Integer readUsage(String key) {
        String value = redisTemplate.opsForValue().get(key);
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private String buildKey(Long salonId, LocalDate quotaDate) {
        return KEY_PREFIX + ":" + salonId + ":" + quotaDate;
    }

    private long secondsUntilNextReset(LocalDate quotaDate) {
        LocalDateTime nextMidnight = quotaDate.plusDays(1).atStartOfDay();
        return Math.max(1L, Duration.between(LocalDateTime.now(QUOTA_ZONE), nextMidnight).getSeconds());
    }

    private LocalDateTime nextResetAt(LocalDate quotaDate) {
        return quotaDate.plusDays(1).atStartOfDay();
    }

    private DefaultRedisScript<Long> consumeScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptText("""
                local current = redis.call('GET', KEYS[1])
                local limit = tonumber(ARGV[1])
                local ttl = tonumber(ARGV[2])

                if not current then
                    redis.call('SET', KEYS[1], '1')
                    redis.call('EXPIRE', KEYS[1], ttl)
                    return 1
                end

                local count = tonumber(current)
                if count >= limit then
                    return -1
                end

                local nextCount = redis.call('INCR', KEYS[1])
                local existingTtl = redis.call('TTL', KEYS[1])
                if existingTtl < 0 then
                    redis.call('EXPIRE', KEYS[1], ttl)
                end
                return nextCount
                """);
        return script;
    }
}
