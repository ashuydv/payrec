package com.payrecon.controller;

import com.payrecon.dto.CreateLedgerEntryRequest;
import com.payrecon.dto.LedgerEntryResponse;
import com.payrecon.service.LedgerEntryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/ledger-entries")
public class LedgerEntryController {

    private final LedgerEntryService ledgerEntryService;

    public LedgerEntryController(LedgerEntryService ledgerEntryService) {
        this.ledgerEntryService = ledgerEntryService;
    }

    @PostMapping
    public ResponseEntity<LedgerEntryResponse> ingest(@Valid @RequestBody CreateLedgerEntryRequest request) {
        LedgerEntryResponse response = ledgerEntryService.ingest(request);
        return ResponseEntity.created(URI.create("/api/ledger-entries/" + response.id())).body(response);
    }

    @GetMapping
    public ResponseEntity<List<LedgerEntryResponse>> listByTransactionReference(
            @RequestParam String transactionReference) {
        return ResponseEntity.ok(ledgerEntryService.listByTransactionReference(transactionReference));
    }
}
