package com.payrecon.service;

import com.payrecon.domain.SettlementStatus;
import com.payrecon.dto.PageResponse;
import com.payrecon.dto.SettlementResponse;
import com.payrecon.dto.SettlementRunResult;
import org.springframework.data.domain.Pageable;

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

    SettlementResponse getSettlement(Long id);

    PageResponse<SettlementResponse> listSettlements(Long merchantId, SettlementStatus status, Pageable pageable);

    /** Transitions a settlement to {@code newStatus}. Only OPEN -> FINALIZED is a real transition; FINALIZED -> FINALIZED is a no-op. */
    SettlementResponse updateStatus(Long id, SettlementStatus newStatus);
}
