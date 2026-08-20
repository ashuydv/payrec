package com.payrecon.service.impl;

import com.payrecon.domain.LedgerEntry;
import com.payrecon.domain.Merchant;
import com.payrecon.domain.ReconciliationStatus;
import com.payrecon.domain.Settlement;
import com.payrecon.domain.SettlementStatus;
import com.payrecon.domain.Transaction;
import com.payrecon.exception.ResourceNotFoundException;
import com.payrecon.repository.LedgerEntryRepository;
import com.payrecon.repository.MerchantRepository;
import com.payrecon.repository.SettlementRepository;
import com.payrecon.repository.TransactionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Settles one merchant's candidate transactions for a period, in its own
 * REQUIRES_NEW transaction (same reasoning as TransactionInserter: Spring's
 * @Transactional only applies through the proxy, so this has to be a
 * separate bean rather than a private method on SettlementServiceImpl). This
 * isolation is what lets one merchant's optimistic-lock conflict on
 * Settlement.version — a concurrent settlement run touching the same
 * merchant/period — fail without rolling back every other merchant already
 * processed in this run.
 */
@Component
class SettlementMerchantRunner {

    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final SettlementRepository settlementRepository;
    private final MerchantRepository merchantRepository;
    private final ReconciliationClassifier classifier;

    SettlementMerchantRunner(
            TransactionRepository transactionRepository,
            LedgerEntryRepository ledgerEntryRepository,
            SettlementRepository settlementRepository,
            MerchantRepository merchantRepository,
            ReconciliationClassifier classifier) {
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.settlementRepository = settlementRepository;
        this.merchantRepository = merchantRepository;
        this.classifier = classifier;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    Outcome settleMerchant(Long merchantId, String period, List<Long> candidateTransactionIds) {
        List<Transaction> candidates = transactionRepository.findAllById(candidateTransactionIds);

        BigDecimal matchedTotal = BigDecimal.ZERO;
        List<Transaction> matched = new ArrayList<>();
        for (Transaction transaction : candidates) {
            List<LedgerEntry> entries = ledgerEntryRepository.findByTransactionReference(transaction.getExternalReference());
            if (classifier.classify(transaction, entries) == ReconciliationStatus.MATCHED) {
                matched.add(transaction);
                matchedTotal = matchedTotal.add(transaction.getAmount());
            }
        }

        if (matched.isEmpty()) {
            return Outcome.noMatchedTransactions();
        }

        Settlement settlement = settlementRepository.findByMerchantIdAndPeriod(merchantId, period).orElse(null);
        if (settlement != null && settlement.getStatus() == SettlementStatus.FINALIZED) {
            return Outcome.skippedFinalized();
        }

        if (settlement == null) {
            settlement = insertNewSettlement(merchantId, period, matchedTotal);
        } else {
            settlement.setTotalAmount(settlement.getTotalAmount().add(matchedTotal));
        }

        for (Transaction transaction : matched) {
            transaction.setSettlement(settlement);
        }

        return Outcome.settled(matched.size());
    }

    /**
     * Handles the same create-vs-concurrent-create race as
     * TransactionInserter: two settlement runs for a never-before-settled
     * merchant/period could both find no existing row and both try to insert.
     * The unique constraint on (merchant_id, period) is the source of truth;
     * losing the race means falling back to the update path against the
     * winner's row.
     */
    private Settlement insertNewSettlement(Long merchantId, String period, BigDecimal matchedTotal) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant not found: " + merchantId));

        try {
            return settlementRepository.saveAndFlush(new Settlement(merchant, period, matchedTotal));
        } catch (DataIntegrityViolationException e) {
            Settlement existing = settlementRepository.findByMerchantIdAndPeriod(merchantId, period)
                    .orElseThrow(() -> e);
            existing.setTotalAmount(existing.getTotalAmount().add(matchedTotal));
            return existing;
        }
    }

    record Outcome(Result result, int transactionsSettled) {
        enum Result { SETTLED, SKIPPED_FINALIZED, NO_MATCHED_TRANSACTIONS }

        static Outcome settled(int count) {
            return new Outcome(Result.SETTLED, count);
        }

        static Outcome skippedFinalized() {
            return new Outcome(Result.SKIPPED_FINALIZED, 0);
        }

        static Outcome noMatchedTransactions() {
            return new Outcome(Result.NO_MATCHED_TRANSACTIONS, 0);
        }
    }
}
