package com.payrecon.service;

import com.payrecon.domain.TransactionStatus;
import com.payrecon.dto.CreateTransactionRequest;
import com.payrecon.dto.CreateTransactionResult;
import com.payrecon.dto.PageResponse;
import com.payrecon.dto.TransactionResponse;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

public interface TransactionService {

    CreateTransactionResult createTransaction(CreateTransactionRequest request);

    TransactionResponse getTransaction(Long id);

    PageResponse<TransactionResponse> listTransactions(
            TransactionStatus status, Long merchantId, Instant from, Instant to, Pageable pageable);

    TransactionResponse updateStatus(Long id, TransactionStatus newStatus);
}
