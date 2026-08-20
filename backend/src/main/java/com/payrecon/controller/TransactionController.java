package com.payrecon.controller;

import com.payrecon.domain.TransactionStatus;
import com.payrecon.dto.CreateTransactionRequest;
import com.payrecon.dto.CreateTransactionResult;
import com.payrecon.dto.PageResponse;
import com.payrecon.dto.TransactionResponse;
import com.payrecon.dto.UpdateTransactionStatusRequest;
import com.payrecon.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(@Valid @RequestBody CreateTransactionRequest request) {
        CreateTransactionResult result = transactionService.createTransaction(request);
        TransactionResponse response = result.transaction();

        if (!result.created()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.created(URI.create("/api/transactions/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable Long id) {
        return ResponseEntity.ok(transactionService.getTransaction(id));
    }

    @GetMapping
    public ResponseEntity<PageResponse<TransactionResponse>> listTransactions(
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) Long merchantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(transactionService.listTransactions(status, merchantId, from, to, pageable));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TransactionResponse> updateStatus(
            @PathVariable Long id, @Valid @RequestBody UpdateTransactionStatusRequest request) {
        return ResponseEntity.ok(transactionService.updateStatus(id, request.status()));
    }
}
