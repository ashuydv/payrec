package com.payrecon.reconciliation.rule;

import com.payrecon.domain.Transaction;
import com.payrecon.domain.LedgerEntry;
import com.payrecon.reconciliation.ReconciliationResult;

/**
 * Strategy interface for deciding whether an internal transaction and its
 * matching bank feed ledger entry agree. Different rules trade off strictness
 * for tolerance of real-world noise (rounding, fees) — see ExactMatchRule and
 * TolerantMatchRule.
 */
public interface ReconciliationRule {

    /**
     * @param transaction the internal transaction being reconciled
     * @param bankEntry   the matching BANK_FEED ledger entry, or null if none was found
     */
    ReconciliationResult evaluate(Transaction transaction, LedgerEntry bankEntry);

    String name();
}
