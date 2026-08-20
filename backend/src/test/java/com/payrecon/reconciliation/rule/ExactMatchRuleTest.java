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

class ExactMatchRuleTest {

    private final ExactMatchRule rule = new ExactMatchRule();
    private final Merchant merchant = new Merchant("Acme", "ACC-1");

    @Test
    void evaluate_returnsMatched_whenAmountsAreIdentical() {
        Transaction transaction = new Transaction(merchant, new BigDecimal("100.00"), "USD", "ext-1");
        LedgerEntry bankEntry = new LedgerEntry("ext-1", new BigDecimal("100.00"), LedgerSource.BANK_FEED);

        ReconciliationResult result = rule.evaluate(transaction, bankEntry);

        assertThat(result.outcome()).isEqualTo(ReconciliationOutcome.MATCHED);
    }

    @Test
    void evaluate_returnsMismatched_whenAmountsDifferByAnyAmount() {
        Transaction transaction = new Transaction(merchant, new BigDecimal("100.00"), "USD", "ext-1");
        LedgerEntry bankEntry = new LedgerEntry("ext-1", new BigDecimal("100.01"), LedgerSource.BANK_FEED);

        ReconciliationResult result = rule.evaluate(transaction, bankEntry);

        assertThat(result.outcome()).isEqualTo(ReconciliationOutcome.MISMATCHED);
        assertThat(result.discrepancy()).contains("100.00").contains("100.01");
    }

    @Test
    void evaluate_returnsMissing_whenNoBankEntry() {
        Transaction transaction = new Transaction(merchant, new BigDecimal("100.00"), "USD", "ext-1");

        ReconciliationResult result = rule.evaluate(transaction, null);

        assertThat(result.outcome()).isEqualTo(ReconciliationOutcome.MISSING);
        assertThat(result.bankAmount()).isNull();
    }
}
