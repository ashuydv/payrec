package com.payrecon.reconciliation.impl;

import com.payrecon.domain.LedgerEntry;
import com.payrecon.domain.LedgerSource;
import com.payrecon.domain.Merchant;
import com.payrecon.domain.Transaction;
import com.payrecon.reconciliation.ReconciliationOutcome;
import com.payrecon.reconciliation.ReconciliationResult;
import com.payrecon.reconciliation.rule.ExactMatchRule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class ReconciliationServiceImplTest {

    private final ReconciliationServiceImpl service = new ReconciliationServiceImpl();
    private final Merchant merchant = new Merchant("Acme", "ACC-1");

    @Test
    void reconcile_matchesEachTransactionToItsBankEntryByReference() {
        Transaction matched = new Transaction(merchant, new BigDecimal("50.00"), "USD", "ext-match");
        Transaction mismatched = new Transaction(merchant, new BigDecimal("75.00"), "USD", "ext-mismatch");
        Transaction missing = new Transaction(merchant, new BigDecimal("20.00"), "USD", "ext-missing");

        List<LedgerEntry> bankFeed = List.of(
                new LedgerEntry("ext-match", new BigDecimal("50.00"), LedgerSource.BANK_FEED),
                new LedgerEntry("ext-mismatch", new BigDecimal("70.00"), LedgerSource.BANK_FEED)
        );

        List<ReconciliationResult> results = service.reconcile(
                List.of(matched, mismatched, missing), bankFeed, new ExactMatchRule());

        assertThat(results)
                .extracting(ReconciliationResult::transactionReference, ReconciliationResult::outcome)
                .containsExactly(
                        tuple("ext-match", ReconciliationOutcome.MATCHED),
                        tuple("ext-mismatch", ReconciliationOutcome.MISMATCHED),
                        tuple("ext-missing", ReconciliationOutcome.MISSING)
                );
    }

    @Test
    void reconcile_keepsFirstBankEntry_whenDuplicateReferencesExistInBankFeed() {
        Transaction transaction = new Transaction(merchant, new BigDecimal("10.00"), "USD", "ext-dup");
        List<LedgerEntry> bankFeed = List.of(
                new LedgerEntry("ext-dup", new BigDecimal("10.00"), LedgerSource.BANK_FEED),
                new LedgerEntry("ext-dup", new BigDecimal("99.00"), LedgerSource.BANK_FEED)
        );

        List<ReconciliationResult> results = service.reconcile(List.of(transaction), bankFeed, new ExactMatchRule());

        assertThat(results).hasSize(1);
        assertThat(results.get(0).outcome()).isEqualTo(ReconciliationOutcome.MATCHED);
    }
}
