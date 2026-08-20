package com.payrecon.service.impl;

import com.payrecon.domain.Transaction;
import com.payrecon.domain.TransactionStatus;
import com.payrecon.dto.CreateTransactionRequest;
import com.payrecon.dto.CreateTransactionResult;
import com.payrecon.dto.PageResponse;
import com.payrecon.dto.TransactionResponse;
import com.payrecon.exception.InvalidStatusTransitionException;
import com.payrecon.exception.ResourceNotFoundException;
import com.payrecon.repository.TransactionRepository;
import com.payrecon.repository.TransactionSpecifications;
import com.payrecon.service.TransactionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

/*
 * Idempotency strategy
 * =====================
 * Transaction creation is idempotent on `externalReference` (the id the
 * upstream payment feed/caller supplies, e.g. a gateway charge id). A caller
 * that retries a create call after a timeout — never knowing whether the
 * first attempt actually succeeded — must be able to POST the same request
 * again and safely get back the *same* transaction rather than a duplicate
 * charge record.
 *
 * We deliberately do NOT implement this as "SELECT by externalReference,
 * insert if absent" in application code: under concurrent requests (two
 * retries racing, or a duplicate webhook delivery) that check-then-act has a
 * TOCTOU race — both requests can pass the SELECT before either INSERT
 * commits, producing two rows for the same externalReference.
 *
 * Instead, the database's unique constraint on transactions.external_reference
 * (see Transaction entity) is the single source of truth. We optimistically
 * attempt the insert; if it violates the unique constraint, we know another
 * request already created the row (whether just now or earlier), so we
 * re-fetch it and return it with 200 OK instead of 201 Created. This pushes
 * the correctness guarantee down to the database, which is the only place
 * that can atomically enforce "at most one row per externalReference" under
 * concurrency.
 *
 * The actual insert-and-catch-the-constraint-violation logic lives in
 * TransactionInserter rather than here, because it needs its own
 * REQUIRES_NEW transaction boundary — and Spring's @Transactional only
 * works through the proxy, which self-invocation within one class bypasses.
 */
@Service
public class TransactionServiceImpl implements TransactionService {

    private static final Set<TransactionStatus> VALID_TRANSITIONS_FROM_PENDING =
            Set.of(TransactionStatus.PROCESSED, TransactionStatus.FAILED, TransactionStatus.DISPUTED);

    private final TransactionRepository transactionRepository;
    private final TransactionInserter transactionInserter;

    public TransactionServiceImpl(TransactionRepository transactionRepository, TransactionInserter transactionInserter) {
        this.transactionRepository = transactionRepository;
        this.transactionInserter = transactionInserter;
    }

    @Override
    public CreateTransactionResult createTransaction(CreateTransactionRequest request) {
        return transactionRepository.findByExternalReference(request.externalReference())
                .map(existing -> new CreateTransactionResult(TransactionResponse.from(existing), false))
                .orElseGet(() -> {
                    TransactionInserter.InsertOutcome outcome = transactionInserter.insertNewTransaction(request);
                    return new CreateTransactionResult(TransactionResponse.from(outcome.transaction()), outcome.created());
                });
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(Long id) {
        return transactionRepository.findById(id)
                .map(TransactionResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> listTransactions(
            TransactionStatus status, Long merchantId, Instant from, Instant to, Pageable pageable) {
        Page<TransactionResponse> page = transactionRepository
                .findAll(TransactionSpecifications.filter(status, merchantId, from, to), pageable)
                .map(TransactionResponse::from);
        return PageResponse.from(page);
    }

    @Override
    @Transactional
    public TransactionResponse updateStatus(Long id, TransactionStatus newStatus) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + id));

        validateTransition(transaction.getStatus(), newStatus);
        transaction.setStatus(newStatus);
        return TransactionResponse.from(transaction);
    }

    private void validateTransition(TransactionStatus current, TransactionStatus next) {
        if (current == next) {
            return;
        }
        boolean allowed = switch (current) {
            case PENDING -> VALID_TRANSITIONS_FROM_PENDING.contains(next);
            case FAILED -> next == TransactionStatus.PENDING || next == TransactionStatus.PROCESSED;
            case PROCESSED, DISPUTED -> next == TransactionStatus.DISPUTED;
        };

        if (!allowed) {
            throw new InvalidStatusTransitionException(
                    "Cannot transition transaction from %s to %s".formatted(current, next));
        }
    }
}
