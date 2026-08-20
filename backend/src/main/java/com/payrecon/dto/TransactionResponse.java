package com.payrecon.dto;

import com.payrecon.domain.PaymentType;
import com.payrecon.domain.Transaction;
import com.payrecon.domain.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
        Long id,
        Long merchantId,
        String merchantName,
        BigDecimal amount,
        String currency,
        TransactionStatus status,
        PaymentType paymentType,
        String externalReference,
        Instant createdAt,
        Instant processedAt,
        int retryCount,
        Instant nextRetryAt
) {
    public static TransactionResponse from(Transaction t) {
        return new TransactionResponse(
                t.getId(),
                t.getMerchant().getId(),
                t.getMerchant().getName(),
                t.getAmount(),
                t.getCurrency(),
                t.getStatus(),
                t.getPaymentType(),
                t.getExternalReference(),
                t.getCreatedAt(),
                t.getProcessedAt(),
                t.getRetryCount(),
                t.getNextRetryAt()
        );
    }
}
