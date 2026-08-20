package com.payrecon.batch;

import com.payrecon.domain.LedgerEntry;
import com.payrecon.domain.LedgerSource;
import com.payrecon.domain.ReconciliationReport;
import com.payrecon.domain.Transaction;
import com.payrecon.reconciliation.ReconciliationResult;
import com.payrecon.reconciliation.rule.ReconciliationRule;
import com.payrecon.repository.LedgerEntryRepository;
import org.springframework.batch.item.ItemProcessor;

import java.time.LocalDate;

/**
 * Reconciles a single transaction against the bank feed for the nightly job.
 * Runs per-item rather than batching through ReconciliationService (which is
 * built for the whole-list REST preview use case) because Spring Batch chunk
 * processing is inherently one-item-at-a-time.
 */
public class ReconciliationItemProcessor implements ItemProcessor<Transaction, ReconciliationReport> {

    private final LedgerEntryRepository ledgerEntryRepository;
    private final ReconciliationRule rule;
    private final LocalDate runDate;

    public ReconciliationItemProcessor(LedgerEntryRepository ledgerEntryRepository, ReconciliationRule rule, LocalDate runDate) {
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.rule = rule;
        this.runDate = runDate;
    }

    @Override
    public ReconciliationReport process(Transaction transaction) {
        LedgerEntry bankEntry = ledgerEntryRepository.findByTransactionReference(transaction.getExternalReference())
                .stream()
                .filter(entry -> entry.getSource() == LedgerSource.BANK_FEED)
                .findFirst()
                .orElse(null);

        ReconciliationResult result = rule.evaluate(transaction, bankEntry);

        return new ReconciliationReport(
                runDate,
                result.transactionReference(),
                result.outcome(),
                result.internalAmount(),
                result.bankAmount(),
                result.discrepancy()
        );
    }
}
