package com.learnify.ratelimit;

import com.learnify.entity.GenerationRequestLog;
import com.learnify.repository.GenerationRequestLogRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sliding-window-log rate limiter: max MAX_REQUESTS per WINDOW per user, backed by a
 * plain table rather than Redis (BACKEND_PLAN.md Extendability §5 — no caching layer yet).
 */
@Component
public class SlidingWindowRateLimiter {

    private static final int MAX_REQUESTS = 2;
    private static final Duration WINDOW = Duration.ofSeconds(60);

    private final GenerationRequestLogRepository repository;

    public SlidingWindowRateLimiter(GenerationRequestLogRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void checkAndRecord(UUID userId) {
        Instant windowStart = Instant.now().minus(WINDOW);
        long count = repository.countByUserIdAndCreatedAtAfter(userId, windowStart);
        if (count >= MAX_REQUESTS) {
            throw new RateLimitExceededException("Too many generation requests — try again in a minute");
        }
        GenerationRequestLog log = new GenerationRequestLog();
        log.setUserId(userId);
        repository.save(log);
    }
}
