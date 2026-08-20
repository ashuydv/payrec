package com.payrecon.controller;

import com.payrecon.dto.ReconciliationResponse;
import com.payrecon.service.ReconciliationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions/{id}/reconciliation")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    public ReconciliationController(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @GetMapping
    public ResponseEntity<ReconciliationResponse> reconcile(@PathVariable Long id) {
        return ResponseEntity.ok(reconciliationService.reconcile(id));
    }
}
