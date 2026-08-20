package com.payrecon.retry;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Models exponential backoff for retrying FAILED transactions:
 * nextRetryAt = now + baseDelay * 2^retryCount, capped at maxRetries
 * attempts. We only *compute* the schedule here — we don't sleep or actually
 * wait for it, since retries are triggered externally (via the retry
 * endpoint, or eventually a scheduled job that polls for due retries).
 */
@Component
public class RetryPolicy {

    public static final int MAX_RETRIES = 5;
    private static final Duration BASE_DELAY = Duration.ofMinutes(1);

    public int maxRetries() {
        return MAX_RETRIES;
    }

    public boolean canRetry(int currentRetryCount) {
        return currentRetryCount < MAX_RETRIES;
    }

    public Instant nextRetryAt(int retryCountAfterThisAttempt) {
        long delaySeconds = BASE_DELAY.getSeconds() * (1L << retryCountAfterThisAttempt);
        return Instant.now().plusSeconds(delaySeconds);
    }
}
