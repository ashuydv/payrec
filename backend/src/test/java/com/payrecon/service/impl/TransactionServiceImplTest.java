package com.payrecon.service.impl;

import com.payrecon.domain.Merchant;
import com.payrecon.domain.PaymentType;
import com.payrecon.domain.Transaction;
import com.payrecon.domain.TransactionStatus;
import com.payrecon.dto.CreateTransactionRequest;
import com.payrecon.dto.CreateTransactionResult;
import com.payrecon.exception.InvalidStatusTransitionException;
import com.payrecon.exception.ResourceNotFoundException;
import com.payrecon.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionInserter transactionInserter;

    private TransactionServiceImpl service;

    private Merchant merchant;

    @BeforeEach
    void setUp() {
        service = new TransactionServiceImpl(transactionRepository, transactionInserter);
        merchant = new Merchant("Acme Co", "ACC-001");
        setId(merchant, 1L);
    }

    @Test
    void createTransaction_insertsNewRow_whenExternalReferenceUnseen() {
        CreateTransactionRequest request = new CreateTransactionRequest(1L, new BigDecimal("100.00"), "USD", "ext-123", PaymentType.CARD);
        Transaction saved = new Transaction(merchant, request.amount(), request.currency(), request.externalReference());
        setId(saved, 42L);

        when(transactionRepository.findByExternalReference("ext-123")).thenReturn(Optional.empty());
        when(transactionInserter.insertNewTransaction(request))
                .thenReturn(new TransactionInserter.InsertOutcome(saved, true));

        CreateTransactionResult result = service.createTransaction(request);

        assertThat(result.created()).isTrue();
        assertThat(result.transaction().id()).isEqualTo(42L);
        assertThat(result.transaction().externalReference()).isEqualTo("ext-123");
    }

    @Test
    void createTransaction_returnsExistingRow_whenExternalReferenceAlreadyExists() {
        CreateTransactionRequest request = new CreateTransactionRequest(1L, new BigDecimal("100.00"), "USD", "ext-123", PaymentType.CARD);
        Transaction existing = new Transaction(merchant, new BigDecimal("100.00"), "USD", "ext-123");
        setId(existing, 7L);

        when(transactionRepository.findByExternalReference("ext-123")).thenReturn(Optional.of(existing));

        CreateTransactionResult result = service.createTransaction(request);

        assertThat(result.created()).isFalse();
        assertThat(result.transaction().id()).isEqualTo(7L);
        verify(transactionInserter, never()).insertNewTransaction(any());
    }

    @Test
    void createTransaction_returnsExistingRow_whenInserterLosesRaceToConcurrentRequest() {
        // Simulates: findByExternalReference sees nothing yet (not-created-yet window),
        // but by the time the insert runs, a concurrent request has already committed it.
        CreateTransactionRequest request = new CreateTransactionRequest(1L, new BigDecimal("50.00"), "USD", "ext-race", PaymentType.CARD);
        Transaction winnerRow = new Transaction(merchant, new BigDecimal("50.00"), "USD", "ext-race");
        setId(winnerRow, 99L);

        when(transactionRepository.findByExternalReference("ext-race")).thenReturn(Optional.empty());
        when(transactionInserter.insertNewTransaction(request))
                .thenReturn(new TransactionInserter.InsertOutcome(winnerRow, false));

        CreateTransactionResult result = service.createTransaction(request);

        assertThat(result.created()).isFalse();
        assertThat(result.transaction().id()).isEqualTo(99L);
    }

    @Test
    void getTransaction_throwsNotFound_whenMissing() {
        when(transactionRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTransaction(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateStatus_allowsPendingToProcessed() {
        Transaction transaction = new Transaction(merchant, new BigDecimal("10.00"), "USD", "ext-1");
        setId(transaction, 1L);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));

        var response = service.updateStatus(1L, TransactionStatus.PROCESSED);

        assertThat(response.status()).isEqualTo(TransactionStatus.PROCESSED);
        assertThat(response.processedAt()).isNotNull();
    }

    @Test
    void updateStatus_rejectsProcessedToPending() {
        Transaction transaction = new Transaction(merchant, new BigDecimal("10.00"), "USD", "ext-1");
        transaction.setStatus(TransactionStatus.PROCESSED);
        setId(transaction, 1L);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));

        assertThatThrownBy(() -> service.updateStatus(1L, TransactionStatus.PENDING))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void updateStatus_allowsProcessedToDisputed() {
        Transaction transaction = new Transaction(merchant, new BigDecimal("10.00"), "USD", "ext-1");
        transaction.setStatus(TransactionStatus.PROCESSED);
        setId(transaction, 1L);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));

        var response = service.updateStatus(1L, TransactionStatus.DISPUTED);

        assertThat(response.status()).isEqualTo(TransactionStatus.DISPUTED);
    }

    /** Entities use DB-generated ids, so tests set them via reflection rather than exposing a setter. */
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
