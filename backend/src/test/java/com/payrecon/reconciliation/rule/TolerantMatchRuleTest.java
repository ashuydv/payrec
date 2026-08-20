package com.payrecon.reconciliation.rule;

import com.payrecon.domain.LedgerEntry;
import com.payrecon.domain.LedgerSource;
import com.payrecon.domain.Merchant;
import com.payrecon.domain.Transaction;
import com.payrecon.reconciliation.ReconciliationOutcome;
import com.payrecon.reconciliation.ReconciliationResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TolerantMatchRuleTest {

    private final TolerantMatchRule rule = new TolerantMatchRule(new BigDecimal("0.02"));
    private final Merchant merchant = new Merchant("Acme", "ACC-1");

    @Test
    void evaluate_returnsMatched_whenAmountsAreIdentical() {
        Transaction transaction = new Transaction(merchant, new BigDecimal("100.00"), "USD", "ext-1");
        LedgerEntry bankEntry = new LedgerEntry("ext-1", new BigDecimal("100.00"), LedgerSource.BANK_FEED);

        assertThat(rule.evaluate(transaction, bankEntry).outcome()).isEqualTo(ReconciliationOutcome.MATCHED);
    }

    @Test
    void evaluate_returnsMatched_whenDifferenceIsWithinTolerance() {
        Transaction transaction = new Transaction(merchant, new BigDecimal("100.00"), "USD", "ext-1");
        LedgerEntry bankEntry = new LedgerEntry("ext-1", new BigDecimal("99.99"), LedgerSource.BANK_FEED);

        ReconciliationResult result = rule.evaluate(transaction, bankEntry);

        assertThat(result.outcome()).isEqualTo(ReconciliationOutcome.MATCHED);
    }

    @Test
    void evaluate_returnsMatched_whenDifferenceEqualsToleranceExactly() {
        Transaction transaction = new Transaction(merchant, new BigDecimal("100.00"), "USD", "ext-1");
        LedgerEntry bankEntry = new LedgerEntry("ext-1", new BigDecimal("100.02"), LedgerSource.BANK_FEED);

        assertThat(rule.evaluate(transaction, bankEntry).outcome()).isEqualTo(ReconciliationOutcome.MATCHED);
    }

    @Test
    void evaluate_returnsMismatched_whenDifferenceExceedsTolerance() {
        Transaction transaction = new Transaction(merchant, new BigDecimal("100.00"), "USD", "ext-1");
        LedgerEntry bankEntry = new LedgerEntry("ext-1", new BigDecimal("100.50"), LedgerSource.BANK_FEED);

        ReconciliationResult result = rule.evaluate(transaction, bankEntry);

        assertThat(result.outcome()).isEqualTo(ReconciliationOutcome.MISMATCHED);
    }

    @Test
    void evaluate_returnsMissing_whenNoBankEntry() {
        Transaction transaction = new Transaction(merchant, new BigDecimal("100.00"), "USD", "ext-1");

        assertThat(rule.evaluate(transaction, null).outcome()).isEqualTo(ReconciliationOutcome.MISSING);
    }
}
