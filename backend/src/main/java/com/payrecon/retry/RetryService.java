package com.payrecon.retry;

import com.payrecon.dto.TransactionResponse;

public interface RetryService {

    /**
     * Retries a FAILED transaction: re-attempts processing, and on failure
     * advances retryCount / nextRetryAt per the configured backoff policy.
     * Throws MaxRetriesExceededException if the transaction has already
     * exhausted its retry budget.
     */
    TransactionResponse retry(Long transactionId);
}
