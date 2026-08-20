package com.payrecon.dto;

import com.payrecon.domain.ReconciliationStatus;

import java.math.BigDecimal;
import java.util.List;

public record ReconciliationResponse(
        Long transactionId,
        String externalReference,
        BigDecimal transactionAmount,
        ReconciliationStatus status,
        List<LedgerEntryResponse> ledgerEntries
) {
}
