package com.payrecon.controller;

import com.payrecon.dto.SettlementResponse;
import com.payrecon.dto.SettlementSummaryRow;
import com.payrecon.repository.SettlementRepository;
import com.payrecon.settlement.SettlementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/settlements")
public class SettlementController {

    private final SettlementService settlementService;
    private final SettlementRepository settlementRepository;

    public SettlementController(SettlementService settlementService, SettlementRepository settlementRepository) {
        this.settlementService = settlementService;
        this.settlementRepository = settlementRepository;
    }

    @PostMapping("/merchants/{merchantId}/periods/{period}/run")
    public ResponseEntity<SettlementResponse> runSettlement(
            @PathVariable Long merchantId, @PathVariable String period) {
        return ResponseEntity.ok(settlementService.runSettlement(merchantId, period));
    }

    @GetMapping("/merchants/{merchantId}/periods/{period}")
    public ResponseEntity<SettlementResponse> getSettlement(
            @PathVariable Long merchantId, @PathVariable String period) {
        return ResponseEntity.ok(settlementService.getSettlement(merchantId, period));
    }

    @GetMapping("/summary")
    public ResponseEntity<List<SettlementSummaryRow>> summary(
            @RequestParam(required = false) Long merchantId) {
        return ResponseEntity.ok(settlementRepository.summarize(merchantId));
    }

    @GetMapping("/summary/stored-procedure")
    public ResponseEntity<List<SettlementSummaryRow>> summaryViaStoredProcedure(
            @RequestParam(required = false) Long merchantId) {
        return ResponseEntity.ok(settlementRepository.summarizeViaStoredProcedure(merchantId));
    }
}
