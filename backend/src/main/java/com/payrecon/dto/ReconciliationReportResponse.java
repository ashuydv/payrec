package com.payrecon.dto;

import com.payrecon.domain.ReconciliationOutcome;
import com.payrecon.domain.ReconciliationReport;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ReconciliationReportResponse(
        Long id,
        LocalDate runDate,
        String transactionReference,
        Long transactionId,
        ReconciliationOutcome outcome,
        BigDecimal internalAmount,
        BigDecimal bankAmount,
        String discrepancy,
        boolean needsReview,
        Instant createdAt
) {
    public static ReconciliationReportResponse from(ReconciliationReport r, Long transactionId) {
        return new ReconciliationReportResponse(
                r.getId(),
                r.getRunDate(),
                r.getTransactionReference(),
                transactionId,
                r.getOutcome(),
                r.getInternalAmount(),
                r.getBankAmount(),
                r.getDiscrepancy(),
                r.isNeedsReview(),
                r.getCreatedAt()
        );
    }
}
