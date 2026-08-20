package com.payrecon.controller;

import com.payrecon.domain.SettlementStatus;
import com.payrecon.dto.PageResponse;
import com.payrecon.dto.SettlementResponse;
import com.payrecon.dto.UpdateSettlementStatusRequest;
import com.payrecon.service.SettlementService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settlements")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<SettlementResponse> getSettlement(@PathVariable Long id) {
        return ResponseEntity.ok(settlementService.getSettlement(id));
    }

    @GetMapping
    public ResponseEntity<PageResponse<SettlementResponse>> listSettlements(
            @RequestParam(required = false) Long merchantId,
            @RequestParam(required = false) SettlementStatus status,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(settlementService.listSettlements(merchantId, status, pageable));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<SettlementResponse> updateStatus(
            @PathVariable Long id, @Valid @RequestBody UpdateSettlementStatusRequest request) {
        return ResponseEntity.ok(settlementService.updateStatus(id, request.status()));
    }
}
