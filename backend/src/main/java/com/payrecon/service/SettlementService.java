package com.payrecon.service;

import com.payrecon.dto.SettlementRunResult;

public interface SettlementService {

    /**
     * Aggregates every not-yet-settled, PROCESSED transaction whose
     * processedAt falls on {@code period} (an ISO local date, e.g.
     * "2026-08-19") into per-merchant Settlement rows, but only counts
     * transactions that are reconciliation-MATCHED. Safe to re-run for the
     * same period: already-settled transactions are excluded, so a rerun
     * only picks up transactions that have since reconciled.
     */
    SettlementRunResult runSettlement(String period);
}
