package com.payrecon.domain;

/**
 * Outcome of comparing a transaction against the ledger entries recorded
 * against its externalReference. Computed on demand from Transaction +
 * LedgerEntry rather than persisted — see docs/phase-3-reconciliation-engine.md.
 */
public enum ReconciliationStatus {

    /** At least one INTERNAL and one BANK_FEED entry exist, and every entry's amount agrees with the transaction. */
    MATCHED,

    /** One or more ledger entries exist but at least one disagrees with the transaction amount. */
    AMOUNT_MISMATCH,

    /** Ledger entries exist and agree on amount, but none of them is a BANK_FEED entry yet. */
    MISSING_BANK_CONFIRMATION,

    /** No ledger entries have been recorded against this transaction's externalReference. */
    NO_LEDGER_ENTRIES
}
