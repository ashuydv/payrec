package com.payrecon.service.impl;

import com.payrecon.domain.Merchant;
import com.payrecon.domain.Transaction;
import com.payrecon.domain.TransactionStatus;
import com.payrecon.dto.SettlementRunResult;
import com.payrecon.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementServiceImplTest {

    private static final String PERIOD = "2026-08-19";

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private SettlementMerchantRunner merchantRunner;

    private SettlementServiceImpl service;

    private Transaction merchantOneTx;
    private Transaction merchantTwoTx;

    @BeforeEach
    void setUp() {
        service = new SettlementServiceImpl(transactionRepository, merchantRunner);

        Merchant merchantOne = new Merchant("Acme Co", "ACC-001");
        setId(merchantOne, 1L);
        Merchant merchantTwo = new Merchant("Beta Co", "ACC-002");
        setId(merchantTwo, 2L);

        merchantOneTx = new Transaction(merchantOne, new BigDecimal("100.00"), "USD", "ext-1");
        setId(merchantOneTx, 10L);
        merchantTwoTx = new Transaction(merchantTwo, new BigDecimal("50.00"), "USD", "ext-2");
        setId(merchantTwoTx, 20L);

        when(transactionRepository.findByStatusAndSettlementIsNullAndProcessedAtGreaterThanEqualAndProcessedAtLessThan(
                eq(TransactionStatus.PROCESSED), any(), any()))
                .thenReturn(List.of(merchantOneTx, merchantTwoTx));
    }

    @Test
    void runSettlement_queriesTheUtcDayWindowForThePeriod() {
        when(merchantRunner.settleMerchant(any(), any(), any()))
                .thenReturn(SettlementMerchantRunner.Outcome.noMatchedTransactions());

        service.runSettlement(PERIOD);

        ArgumentCaptor<Instant> from = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> to = ArgumentCaptor.forClass(Instant.class);
        org.mockito.Mockito.verify(transactionRepository)
                .findByStatusAndSettlementIsNullAndProcessedAtGreaterThanEqualAndProcessedAtLessThan(
                        eq(TransactionStatus.PROCESSED), from.capture(), to.capture());

        assertThat(from.getValue()).isEqualTo(Instant.parse("2026-08-19T00:00:00Z"));
        assertThat(to.getValue()).isEqualTo(Instant.parse("2026-08-20T00:00:00Z"));
    }

    @Test
    void runSettlement_aggregatesOutcomesAcrossMerchants() {
        when(merchantRunner.settleMerchant(eq(1L), eq(PERIOD), any()))
                .thenReturn(SettlementMerchantRunner.Outcome.settled(1));
        when(merchantRunner.settleMerchant(eq(2L), eq(PERIOD), any()))
                .thenReturn(SettlementMerchantRunner.Outcome.skippedFinalized());

        SettlementRunResult result = service.runSettlement(PERIOD);

        assertThat(result.transactionsConsidered()).isEqualTo(2);
        assertThat(result.merchantsSettled()).isEqualTo(1);
        assertThat(result.transactionsSettled()).isEqualTo(1);
        assertThat(result.merchantsSkippedFinalized()).isEqualTo(1);
        assertThat(result.merchantsSkippedConflict()).isZero();
    }

    @Test
    void runSettlement_countsOptimisticLockConflictWithoutFailingTheRun() {
        when(merchantRunner.settleMerchant(eq(1L), eq(PERIOD), any()))
                .thenThrow(new ObjectOptimisticLockingFailureException("Settlement", 1L));
        when(merchantRunner.settleMerchant(eq(2L), eq(PERIOD), any()))
                .thenReturn(SettlementMerchantRunner.Outcome.settled(1));

        SettlementRunResult result = service.runSettlement(PERIOD);

        assertThat(result.merchantsSkippedConflict()).isEqualTo(1);
        assertThat(result.merchantsSettled()).isEqualTo(1);
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
