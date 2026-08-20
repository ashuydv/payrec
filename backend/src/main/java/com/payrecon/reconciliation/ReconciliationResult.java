package com.payrecon.reconciliation;

import java.math.BigDecimal;

public record ReconciliationResult(
        String transactionReference,
        ReconciliationOutcome outcome,
        BigDecimal internalAmount,
        BigDecimal bankAmount,
        String discrepancy
) {
    public static ReconciliationResult matched(String reference, BigDecimal amount) {
        return new ReconciliationResult(reference, ReconciliationOutcome.MATCHED, amount, amount, null);
    }

    public static ReconciliationResult mismatched(String reference, BigDecimal internalAmount, BigDecimal bankAmount, String discrepancy) {
        return new ReconciliationResult(reference, ReconciliationOutcome.MISMATCHED, internalAmount, bankAmount, discrepancy);
    }

    public static ReconciliationResult missing(String reference, BigDecimal internalAmount) {
        return new ReconciliationResult(reference, ReconciliationOutcome.MISSING, internalAmount, null,
                "No bank feed entry found for reference " + reference);
    }
}
