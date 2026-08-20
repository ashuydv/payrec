package com.payrecon.service.impl;

import com.payrecon.domain.Merchant;
import com.payrecon.domain.Settlement;
import com.payrecon.domain.SettlementStatus;
import com.payrecon.domain.Transaction;
import com.payrecon.domain.TransactionStatus;
import com.payrecon.dto.SettlementResponse;
import com.payrecon.dto.SettlementRunResult;
import com.payrecon.exception.InvalidStatusTransitionException;
import com.payrecon.exception.ResourceNotFoundException;
import com.payrecon.repository.SettlementRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementServiceImplTest {

    private static final String PERIOD = "2026-08-19";

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private SettlementMerchantRunner merchantRunner;

    private SettlementServiceImpl service;

    private Merchant merchantOne;
    private Transaction merchantOneTx;
    private Transaction merchantTwoTx;

    @BeforeEach
    void setUp() {
        service = new SettlementServiceImpl(transactionRepository, settlementRepository, merchantRunner);

        merchantOne = new Merchant("Acme Co", "ACC-001");
        setId(merchantOne, 1L);
        Merchant merchantTwo = new Merchant("Beta Co", "ACC-002");
        setId(merchantTwo, 2L);

        merchantOneTx = new Transaction(merchantOne, new BigDecimal("100.00"), "USD", "ext-1");
        setId(merchantOneTx, 10L);
        merchantTwoTx = new Transaction(merchantTwo, new BigDecimal("50.00"), "USD", "ext-2");
        setId(merchantTwoTx, 20L);

        org.mockito.Mockito.lenient()
                .when(transactionRepository.findByStatusAndSettlementIsNullAndProcessedAtGreaterThanEqualAndProcessedAtLessThan(
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

    @Test
    void getSettlement_throwsNotFound_whenMissing() {
        when(settlementRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSettlement(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getSettlement_returnsResponse_whenPresent() {
        Settlement settlement = new Settlement(merchantOne, PERIOD, new BigDecimal("100.00"));
        setId(settlement, 1L);
        when(settlementRepository.findById(1L)).thenReturn(Optional.of(settlement));

        SettlementResponse response = service.getSettlement(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.merchantId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(SettlementStatus.OPEN);
    }

    @Test
    void updateStatus_allowsOpenToFinalized() {
        Settlement settlement = new Settlement(merchantOne, PERIOD, new BigDecimal("100.00"));
        setId(settlement, 1L);
        when(settlementRepository.findById(1L)).thenReturn(Optional.of(settlement));

        SettlementResponse response = service.updateStatus(1L, SettlementStatus.FINALIZED);

        assertThat(response.status()).isEqualTo(SettlementStatus.FINALIZED);
    }

    @Test
    void updateStatus_isIdempotent_whenAlreadyFinalized() {
        Settlement settlement = new Settlement(merchantOne, PERIOD, new BigDecimal("100.00"));
        settlement.setStatus(SettlementStatus.FINALIZED);
        setId(settlement, 1L);
        when(settlementRepository.findById(1L)).thenReturn(Optional.of(settlement));

        SettlementResponse response = service.updateStatus(1L, SettlementStatus.FINALIZED);

        assertThat(response.status()).isEqualTo(SettlementStatus.FINALIZED);
    }

    @Test
    void updateStatus_rejectsFinalizedToOpen() {
        Settlement settlement = new Settlement(merchantOne, PERIOD, new BigDecimal("100.00"));
        settlement.setStatus(SettlementStatus.FINALIZED);
        setId(settlement, 1L);
        when(settlementRepository.findById(1L)).thenReturn(Optional.of(settlement));

        assertThatThrownBy(() -> service.updateStatus(1L, SettlementStatus.OPEN))
                .isInstanceOf(InvalidStatusTransitionException.class);
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
