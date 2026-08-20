package com.payrecon.service.impl;

import com.payrecon.domain.LedgerEntry;
import com.payrecon.domain.Transaction;
import com.payrecon.dto.LedgerEntryResponse;
import com.payrecon.dto.ReconciliationResponse;
import com.payrecon.exception.ResourceNotFoundException;
import com.payrecon.repository.LedgerEntryRepository;
import com.payrecon.repository.TransactionRepository;
import com.payrecon.service.ReconciliationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReconciliationServiceImpl implements ReconciliationService {

    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final ReconciliationClassifier classifier;

    public ReconciliationServiceImpl(
            TransactionRepository transactionRepository,
            LedgerEntryRepository ledgerEntryRepository,
            ReconciliationClassifier classifier) {
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.classifier = classifier;
    }

    @Override
    @Transactional(readOnly = true)
    public ReconciliationResponse reconcile(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + transactionId));

        List<LedgerEntry> entries = ledgerEntryRepository.findByTransactionReference(transaction.getExternalReference());

        return new ReconciliationResponse(
                transaction.getId(),
                transaction.getExternalReference(),
                transaction.getAmount(),
                classifier.classify(transaction, entries),
                entries.stream().map(LedgerEntryResponse::from).toList()
        );
    }
}
