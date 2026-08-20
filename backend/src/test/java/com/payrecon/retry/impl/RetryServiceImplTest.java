package com.payrecon.retry.impl;

import com.payrecon.domain.Merchant;
import com.payrecon.domain.PaymentType;
import com.payrecon.domain.Transaction;
import com.payrecon.domain.TransactionStatus;
import com.payrecon.dto.TransactionResponse;
import com.payrecon.exception.InvalidStatusTransitionException;
import com.payrecon.exception.MaxRetriesExceededException;
import com.payrecon.processing.PaymentProcessingResult;
import com.payrecon.processing.PaymentProcessor;
import com.payrecon.processing.PaymentProcessorFactory;
import com.payrecon.repository.TransactionRepository;
import com.payrecon.retry.RetryPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetryServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PaymentProcessorFactory paymentProcessorFactory;

    @Mock
    private PaymentProcessor paymentProcessor;

    private RetryServiceImpl service;
    private Merchant merchant;

    @BeforeEach
    void setUp() {
        service = new RetryServiceImpl(transactionRepository, paymentProcessorFactory, new RetryPolicy());
        merchant = new Merchant("Acme", "ACC-1");
        setId(merchant, 1L);
    }

    @Test
    void retry_marksProcessed_whenProcessorSucceeds() {
        Transaction failed = failedTransaction(0);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(failed));
        when(paymentProcessorFactory.getProcessor(PaymentType.CARD)).thenReturn(paymentProcessor);
        when(paymentProcessor.process(failed)).thenReturn(PaymentProcessingResult.success("ok"));

        TransactionResponse response = service.retry(1L);

        assertThat(response.status()).isEqualTo(TransactionStatus.PROCESSED);
        assertThat(response.retryCount()).isEqualTo(1);
        assertThat(response.nextRetryAt()).isNull();
    }

    @Test
    void retry_schedulesNextAttempt_whenProcessorFailsAndBudgetRemains() {
        Transaction failed = failedTransaction(0);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(failed));
        when(paymentProcessorFactory.getProcessor(PaymentType.CARD)).thenReturn(paymentProcessor);
        when(paymentProcessor.process(failed)).thenReturn(PaymentProcessingResult.failure("declined"));

        TransactionResponse response = service.retry(1L);

        assertThat(response.status()).isEqualTo(TransactionStatus.FAILED);
        assertThat(response.retryCount()).isEqualTo(1);
        assertThat(response.nextRetryAt()).isNotNull().isAfter(Instant.now());
    }

    @Test
    void retry_leavesNextRetryAtNull_whenFailureExhaustsBudgetOnThisAttempt() {
        Transaction failed = failedTransaction(RetryPolicy.MAX_RETRIES - 1);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(failed));
        when(paymentProcessorFactory.getProcessor(PaymentType.CARD)).thenReturn(paymentProcessor);
        when(paymentProcessor.process(failed)).thenReturn(PaymentProcessingResult.failure("declined"));

        TransactionResponse response = service.retry(1L);

        assertThat(response.retryCount()).isEqualTo(RetryPolicy.MAX_RETRIES);
        assertThat(response.nextRetryAt()).isNull();
    }

    @Test
    void retry_throwsMaxRetriesExceeded_whenBudgetAlreadyExhausted() {
        Transaction failed = failedTransaction(RetryPolicy.MAX_RETRIES);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(failed));

        assertThatThrownBy(() -> service.retry(1L)).isInstanceOf(MaxRetriesExceededException.class);
        verify(paymentProcessorFactory, never()).getProcessor(any());
    }

    @Test
    void retry_rejectsNonFailedTransaction() {
        Transaction pending = new Transaction(merchant, new BigDecimal("10.00"), "USD", "ext-1");
        setId(pending, 1L);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.retry(1L)).isInstanceOf(InvalidStatusTransitionException.class);
    }

    private Transaction failedTransaction(int retryCount) {
        Transaction transaction = new Transaction(merchant, new BigDecimal("10.00"), "USD", "ext-1", PaymentType.CARD);
        transaction.setStatus(TransactionStatus.FAILED);
        transaction.setRetryCount(retryCount);
        setId(transaction, 1L);
        return transaction;
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
