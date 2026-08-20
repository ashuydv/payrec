package com.payrecon.service.impl;

import com.payrecon.domain.LedgerEntry;
import com.payrecon.domain.LedgerSource;
import com.payrecon.domain.Merchant;
import com.payrecon.domain.Settlement;
import com.payrecon.domain.SettlementStatus;
import com.payrecon.domain.Transaction;
import com.payrecon.repository.LedgerEntryRepository;
import com.payrecon.repository.MerchantRepository;
import com.payrecon.repository.SettlementRepository;
import com.payrecon.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementMerchantRunnerTest {

    private static final String PERIOD = "2026-08-19";

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private MerchantRepository merchantRepository;

    private SettlementMerchantRunner runner;

    private Merchant merchant;
    private Transaction matchedTx;
    private Transaction unmatchedTx;

    @BeforeEach
    void setUp() {
        runner = new SettlementMerchantRunner(
                transactionRepository, ledgerEntryRepository, settlementRepository, merchantRepository, new ReconciliationClassifier());

        merchant = new Merchant("Acme Co", "ACC-001");
        setId(merchant, 1L);

        matchedTx = new Transaction(merchant, new BigDecimal("100.00"), "USD", "ext-1");
        setId(matchedTx, 10L);
        unmatchedTx = new Transaction(merchant, new BigDecimal("50.00"), "USD", "ext-2");
        setId(unmatchedTx, 11L);

        lenient().when(transactionRepository.findAllById(List.of(10L, 11L))).thenReturn(List.of(matchedTx, unmatchedTx));
        lenient().when(ledgerEntryRepository.findByTransactionReference("ext-1")).thenReturn(List.of(
                new LedgerEntry("ext-1", new BigDecimal("100.00"), LedgerSource.INTERNAL),
                new LedgerEntry("ext-1", new BigDecimal("100.00"), LedgerSource.BANK_FEED)));
        // Only an INTERNAL entry: MISSING_BANK_CONFIRMATION, so this one is never eligible to settle.
        when(ledgerEntryRepository.findByTransactionReference("ext-2")).thenReturn(List.of(
                new LedgerEntry("ext-2", new BigDecimal("50.00"), LedgerSource.INTERNAL)));
    }

    @Test
    void settleMerchant_createsNewSettlement_andSettlesOnlyMatchedTransactions() {
        when(settlementRepository.findByMerchantIdAndPeriod(1L, PERIOD)).thenReturn(Optional.empty());
        when(merchantRepository.findById(1L)).thenReturn(Optional.of(merchant));
        when(settlementRepository.saveAndFlush(any(Settlement.class))).thenAnswer(inv -> {
            Settlement settlement = inv.getArgument(0);
            setId(settlement, 100L);
            return settlement;
        });

        SettlementMerchantRunner.Outcome outcome = runner.settleMerchant(1L, PERIOD, List.of(10L, 11L));

        assertThat(outcome.result()).isEqualTo(SettlementMerchantRunner.Outcome.Result.SETTLED);
        assertThat(outcome.transactionsSettled()).isEqualTo(1);
        assertThat(matchedTx.getSettlement()).isNotNull();
        assertThat(matchedTx.getSettlement().getTotalAmount()).isEqualByComparingTo("100.00");
        assertThat(unmatchedTx.getSettlement()).isNull();
    }

    @Test
    void settleMerchant_addsToExistingOpenSettlement() {
        Settlement existing = new Settlement(merchant, PERIOD, new BigDecimal("30.00"));
        setId(existing, 200L);
        when(settlementRepository.findByMerchantIdAndPeriod(1L, PERIOD)).thenReturn(Optional.of(existing));

        SettlementMerchantRunner.Outcome outcome = runner.settleMerchant(1L, PERIOD, List.of(10L, 11L));

        assertThat(outcome.result()).isEqualTo(SettlementMerchantRunner.Outcome.Result.SETTLED);
        assertThat(existing.getTotalAmount()).isEqualByComparingTo("130.00");
        assertThat(matchedTx.getSettlement()).isSameAs(existing);
        verify(merchantRepository, never()).findById(any());
    }

    @Test
    void settleMerchant_skipsFinalizedSettlement_leavingTransactionsUnsettled() {
        Settlement finalized = new Settlement(merchant, PERIOD, new BigDecimal("30.00"));
        finalized.setStatus(SettlementStatus.FINALIZED);
        when(settlementRepository.findByMerchantIdAndPeriod(1L, PERIOD)).thenReturn(Optional.of(finalized));

        SettlementMerchantRunner.Outcome outcome = runner.settleMerchant(1L, PERIOD, List.of(10L, 11L));

        assertThat(outcome.result()).isEqualTo(SettlementMerchantRunner.Outcome.Result.SKIPPED_FINALIZED);
        assertThat(matchedTx.getSettlement()).isNull();
    }

    @Test
    void settleMerchant_returnsNoMatchedTransactions_whenNothingReconciles() {
        when(transactionRepository.findAllById(List.of(11L))).thenReturn(List.of(unmatchedTx));

        SettlementMerchantRunner.Outcome outcome = runner.settleMerchant(1L, PERIOD, List.of(11L));

        assertThat(outcome.result()).isEqualTo(SettlementMerchantRunner.Outcome.Result.NO_MATCHED_TRANSACTIONS);
        verify(settlementRepository, never()).findByMerchantIdAndPeriod(any(), any());
    }

    @Test
    void settleMerchant_fallsBackToUpdate_whenConcurrentInsertWinsTheRace() {
        Settlement winner = new Settlement(merchant, PERIOD, new BigDecimal("20.00"));
        setId(winner, 300L);
        when(settlementRepository.findByMerchantIdAndPeriod(1L, PERIOD))
                .thenReturn(Optional.empty(), Optional.of(winner));
        when(merchantRepository.findById(1L)).thenReturn(Optional.of(merchant));
        when(settlementRepository.saveAndFlush(any(Settlement.class)))
                .thenThrow(new DataIntegrityViolationException("uk_settlement_merchant_period"));

        SettlementMerchantRunner.Outcome outcome = runner.settleMerchant(1L, PERIOD, List.of(10L, 11L));

        assertThat(outcome.result()).isEqualTo(SettlementMerchantRunner.Outcome.Result.SETTLED);
        assertThat(winner.getTotalAmount()).isEqualByComparingTo("120.00");
        assertThat(matchedTx.getSettlement()).isSameAs(winner);
    }

    private static void setId(Object entity, Long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
