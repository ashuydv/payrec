package com.payrecon.controller;

import com.payrecon.dto.ReconciliationReportResponse;
import com.payrecon.reconciliation.ReconciliationReportQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reconciliation-reports")
public class ReconciliationReportController {

    private final ReconciliationReportQueryService reportQueryService;

    public ReconciliationReportController(ReconciliationReportQueryService reportQueryService) {
        this.reportQueryService = reportQueryService;
    }

    @GetMapping("/latest/needs-review")
    public ResponseEntity<List<ReconciliationReportResponse>> latestNeedsReview() {
        return ResponseEntity.ok(reportQueryService.latestNeedsReview());
    }
}
