package com.payrecon.reconciliation;

import com.payrecon.domain.LedgerEntry;
import com.payrecon.domain.Transaction;
import com.payrecon.reconciliation.rule.ReconciliationRule;

import java.util.List;

public interface ReconciliationService {

    /**
     * Reconciles each of the given internal transactions against the supplied
     * bank feed ledger entries using the given rule, matching by
     * externalReference / transactionReference.
     */
    List<ReconciliationResult> reconcile(
            List<Transaction> transactions, List<LedgerEntry> bankFeedEntries, ReconciliationRule rule);
}
