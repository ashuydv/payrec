package com.payrecon.reconciliation.rule;

import com.payrecon.domain.LedgerEntry;
import com.payrecon.domain.Transaction;
import com.payrecon.reconciliation.ReconciliationResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Allows a small absolute discrepancy between the internal amount and the
 * bank feed amount, to absorb currency-conversion rounding or processor fees
 * that legitimately shave a few cents off the settled amount without being a
 * real reconciliation problem.
 */
@Component
public class TolerantMatchRule implements ReconciliationRule {

    private final BigDecimal tolerance;

    public TolerantMatchRule() {
        this(new BigDecimal("0.02"));
    }

    public TolerantMatchRule(BigDecimal tolerance) {
        this.tolerance = tolerance;
    }

    @Override
    public ReconciliationResult evaluate(Transaction transaction, LedgerEntry bankEntry) {
        if (bankEntry == null) {
            return ReconciliationResult.missing(transaction.getExternalReference(), transaction.getAmount());
        }

        BigDecimal difference = transaction.getAmount().subtract(bankEntry.getRecordedAmount()).abs();
        if (difference.compareTo(tolerance) <= 0) {
            return ReconciliationResult.matched(transaction.getExternalReference(), transaction.getAmount());
        }

        return ReconciliationResult.mismatched(
                transaction.getExternalReference(),
                transaction.getAmount(),
                bankEntry.getRecordedAmount(),
                "Amount mismatch beyond tolerance %s: internal=%s bank=%s difference=%s"
                        .formatted(tolerance, transaction.getAmount(), bankEntry.getRecordedAmount(), difference)
        );
    }

    @Override
    public String name() {
        return "TOLERANT_MATCH";
    }
}
