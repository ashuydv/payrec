package com.payrecon.service.impl;

import com.payrecon.domain.LedgerEntry;
import com.payrecon.domain.LedgerSource;
import com.payrecon.domain.ReconciliationStatus;
import com.payrecon.domain.Transaction;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Pure classification logic shared by the on-demand reconciliation endpoint
 * (ReconciliationServiceImpl) and the settlement batch (Phase 4), which
 * needs the same MATCHED test to decide which transactions are eligible to
 * settle but must apply it in bulk rather than through the HTTP-facing
 * service.
 */
@Component
class ReconciliationClassifier {

    ReconciliationStatus classify(Transaction transaction, List<LedgerEntry> entries) {
        if (entries.isEmpty()) {
            return ReconciliationStatus.NO_LEDGER_ENTRIES;
        }

        // compareTo, not equals: LedgerEntry.recordedAmount and Transaction.amount can carry
        // different BigDecimal scales (e.g. 10.0 vs 10.00) for the same monetary value.
        boolean allAmountsMatch = entries.stream()
                .allMatch(entry -> entry.getRecordedAmount().compareTo(transaction.getAmount()) == 0);
        if (!allAmountsMatch) {
            return ReconciliationStatus.AMOUNT_MISMATCH;
        }

        boolean hasBankConfirmation = entries.stream().anyMatch(entry -> entry.getSource() == LedgerSource.BANK_FEED);
        if (!hasBankConfirmation) {
            return ReconciliationStatus.MISSING_BANK_CONFIRMATION;
        }

        return ReconciliationStatus.MATCHED;
    }
}
