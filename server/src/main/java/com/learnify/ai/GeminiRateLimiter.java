package com.learnify.ai;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Paces our own outbound Gemini calls (text generation and TTS both go through this)
 * to stay under Gemini's real per-minute quota, instead of bursting past it and
 * getting 429/503s back — a sliding-window throttle, blocking the caller until a
 * slot is free rather than rejecting outright, since pipeline steps already run in
 * the background (design/components/05-generation-pipeline.md).
 */
@Component
public class GeminiRateLimiter {

    private final int maxRequestsPerMinute;
    private final Deque<Instant> recentCalls = new ArrayDeque<>();

    public GeminiRateLimiter(@Value("${gemini.rate-limit.max-requests-per-minute:5}") int maxRequestsPerMinute) {
        this.maxRequestsPerMinute = maxRequestsPerMinute;
    }

    public synchronized void acquire() {
        while (true) {
            Instant now = Instant.now();
            Instant windowStart = now.minus(Duration.ofMinutes(1));
            while (!recentCalls.isEmpty() && recentCalls.peekFirst().isBefore(windowStart)) {
                recentCalls.pollFirst();
            }

            if (recentCalls.size() < maxRequestsPerMinute) {
                recentCalls.addLast(now);
                return;
            }

            long waitMillis = Duration.between(now, recentCalls.peekFirst().plus(Duration.ofMinutes(1))).toMillis() + 200;
            try {
                wait(Math.max(waitMillis, 200));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
