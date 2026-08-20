package com.payrecon.controller;

import com.payrecon.batch.ReconciliationJobTrigger;
import com.payrecon.dto.BatchJobRunResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/batch")
public class BatchController {

    private final ReconciliationJobTrigger reconciliationJobTrigger;

    public BatchController(ReconciliationJobTrigger reconciliationJobTrigger) {
        this.reconciliationJobTrigger = reconciliationJobTrigger;
    }

    @PostMapping("/reconciliation/run")
    public ResponseEntity<BatchJobRunResponse> runReconciliation() {
        return ResponseEntity.ok(reconciliationJobTrigger.trigger());
    }
}
