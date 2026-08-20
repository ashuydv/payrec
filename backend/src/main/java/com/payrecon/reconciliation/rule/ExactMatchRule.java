package com.payrecon.reconciliation.rule;

import com.payrecon.domain.LedgerEntry;
import com.payrecon.domain.Transaction;
import com.payrecon.reconciliation.ReconciliationResult;
import org.springframework.stereotype.Component;

/**
 * Requires the bank feed amount to match the internal amount exactly
 * (down to scale). Reference matching has already happened by the time this
 * rule runs — it only judges amount agreement.
 */
@Component
public class ExactMatchRule implements ReconciliationRule {

    @Override
    public ReconciliationResult evaluate(Transaction transaction, LedgerEntry bankEntry) {
        if (bankEntry == null) {
            return ReconciliationResult.missing(transaction.getExternalReference(), transaction.getAmount());
        }

        if (transaction.getAmount().compareTo(bankEntry.getRecordedAmount()) == 0) {
            return ReconciliationResult.matched(transaction.getExternalReference(), transaction.getAmount());
        }

        return ReconciliationResult.mismatched(
                transaction.getExternalReference(),
                transaction.getAmount(),
                bankEntry.getRecordedAmount(),
                "Amount mismatch: internal=%s bank=%s".formatted(transaction.getAmount(), bankEntry.getRecordedAmount())
        );
    }

    @Override
    public String name() {
        return "EXACT_MATCH";
    }
}
