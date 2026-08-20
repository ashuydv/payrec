package com.payrecon.service.impl;

import com.payrecon.domain.Merchant;
import com.payrecon.domain.Transaction;
import com.payrecon.dto.CreateTransactionRequest;
import com.payrecon.exception.ResourceNotFoundException;
import com.payrecon.repository.MerchantRepository;
import com.payrecon.repository.TransactionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Split out from TransactionServiceImpl so the REQUIRES_NEW transaction
 * boundary below is enforced by Spring's proxy — self-invocation of an
 * {@code @Transactional} method from within the same bean bypasses the
 * proxy entirely, silently dropping the propagation setting.
 */
@Component
class TransactionInserter {

    private final TransactionRepository transactionRepository;
    private final MerchantRepository merchantRepository;

    TransactionInserter(TransactionRepository transactionRepository, MerchantRepository merchantRepository) {
        this.transactionRepository = transactionRepository;
        this.merchantRepository = merchantRepository;
    }

    /**
     * Runs in its own transaction so that a unique-constraint violation here
     * rolls back only the failed insert attempt, not any work the caller's
     * outer transaction may have already done.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    InsertOutcome insertNewTransaction(CreateTransactionRequest request) {
        Merchant merchant = merchantRepository.findById(request.merchantId())
                .orElseThrow(() -> new ResourceNotFoundException("Merchant not found: " + request.merchantId()));

        Transaction transaction = new Transaction(
                merchant, request.amount(), request.currency(), request.externalReference());

        try {
            return new InsertOutcome(transactionRepository.saveAndFlush(transaction), true);
        } catch (DataIntegrityViolationException e) {
            // Lost the race to a concurrent request with the same externalReference.
            Transaction existing = transactionRepository.findByExternalReference(request.externalReference())
                    .orElseThrow(() -> e);
            return new InsertOutcome(existing, false);
        }
    }

    record InsertOutcome(Transaction transaction, boolean created) {
    }
}
