package com.payrecon.service.impl;

import com.payrecon.domain.LedgerEntry;
import com.payrecon.dto.CreateLedgerEntryRequest;
import com.payrecon.dto.LedgerEntryResponse;
import com.payrecon.repository.LedgerEntryRepository;
import com.payrecon.service.LedgerEntryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Ledger entries are append-only: each row is a source's historical claim
 * about a transaction (our own processing, or the bank's statement feed),
 * not a value we ever correct in place. A wrong entry is resolved by
 * reconciliation flagging a mismatch (Phase 3), never by editing this row.
 */
@Service
public class LedgerEntryServiceImpl implements LedgerEntryService {

    private final LedgerEntryRepository ledgerEntryRepository;

    public LedgerEntryServiceImpl(LedgerEntryRepository ledgerEntryRepository) {
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Override
    @Transactional
    public LedgerEntryResponse ingest(CreateLedgerEntryRequest request) {
        LedgerEntry entry = new LedgerEntry(request.transactionReference(), request.recordedAmount(), request.source());
        return LedgerEntryResponse.from(ledgerEntryRepository.save(entry));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LedgerEntryResponse> listByTransactionReference(String transactionReference) {
        return ledgerEntryRepository.findByTransactionReference(transactionReference).stream()
                .map(LedgerEntryResponse::from)
                .toList();
    }
}
