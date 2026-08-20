package com.payrecon.service.impl;

import com.payrecon.domain.LedgerEntry;
import com.payrecon.domain.LedgerSource;
import com.payrecon.domain.ReconciliationStatus;
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

    public ReconciliationServiceImpl(TransactionRepository transactionRepository, LedgerEntryRepository ledgerEntryRepository) {
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
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
                classify(transaction, entries),
                entries.stream().map(LedgerEntryResponse::from).toList()
        );
    }

    private ReconciliationStatus classify(Transaction transaction, List<LedgerEntry> entries) {
        if (entries.isEmpty()) {
            return ReconciliationStatus.NO_LEDGER_ENTRIES;
        }

        // compareTo, not equals: LedgerEntry.recordedAmount and Transaction.amount can carry
        // different BigDecimal scales (e.g. 10.0 vs 10.00) for the same monetary value.
        boolean allAmountsMatch = entries.stream()
                .allMatch(entry -> entry.getRecordedAmount().compareTo(transaction.getAmount()) == 0);
        if (!allAmountsMatch) {
            return ReconciliationStatus.AMOUNT_MISMATCH;
        }

        boolean hasBankConfirmation = entries.stream().anyMatch(entry -> entry.getSource() == LedgerSource.BANK_FEED);
        if (!hasBankConfirmation) {
            return ReconciliationStatus.MISSING_BANK_CONFIRMATION;
        }

        return ReconciliationStatus.MATCHED;
    }
}
