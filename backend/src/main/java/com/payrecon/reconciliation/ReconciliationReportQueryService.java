package com.payrecon.reconciliation;

import com.payrecon.dto.ReconciliationReportResponse;

import java.util.List;

public interface ReconciliationReportQueryService {

    /** Items needing ops review from the most recent batch run, or empty if no run has happened yet. */
    List<ReconciliationReportResponse> latestNeedsReview();
}
