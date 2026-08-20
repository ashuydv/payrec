package com.payrecon.dto;

import com.payrecon.domain.LedgerEntry;
import com.payrecon.domain.LedgerSource;

import java.math.BigDecimal;
import java.time.Instant;

public record LedgerEntryResponse(
        Long id,
        String transactionReference,
        BigDecimal recordedAmount,
        LedgerSource source,
        Instant createdAt
) {
    public static LedgerEntryResponse from(LedgerEntry entry) {
        return new LedgerEntryResponse(
                entry.getId(),
                entry.getTransactionReference(),
                entry.getRecordedAmount(),
                entry.getSource(),
                entry.getCreatedAt()
        );
    }
}
