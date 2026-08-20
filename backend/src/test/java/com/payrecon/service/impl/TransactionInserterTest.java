package com.payrecon.service.impl;

import com.payrecon.domain.Merchant;
import com.payrecon.domain.PaymentType;
import com.payrecon.domain.Transaction;
import com.payrecon.dto.CreateTransactionRequest;
import com.payrecon.exception.ResourceNotFoundException;
import com.payrecon.repository.MerchantRepository;
import com.payrecon.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionInserterTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private MerchantRepository merchantRepository;

    private TransactionInserter inserter;

    @Test
    void insertNewTransaction_savesRow_whenNoConflict() {
        inserter = new TransactionInserter(transactionRepository, merchantRepository);
        Merchant merchant = new Merchant("Acme", "ACC-1");
        setId(merchant, 1L);
        CreateTransactionRequest request = new CreateTransactionRequest(1L, new BigDecimal("20.00"), "USD", "ext-9", PaymentType.CARD);

        when(merchantRepository.findById(1L)).thenReturn(Optional.of(merchant));
        when(transactionRepository.saveAndFlush(any(Transaction.class)))
                .thenAnswer(inv -> {
                    Transaction t = inv.getArgument(0);
                    setId(t, 55L);
                    return t;
                });

        TransactionInserter.InsertOutcome outcome = inserter.insertNewTransaction(request);

        assertThat(outcome.created()).isTrue();
        assertThat(outcome.transaction().getId()).isEqualTo(55L);
    }

    @Test
    void insertNewTransaction_returnsExisting_whenUniqueConstraintViolated() {
        inserter = new TransactionInserter(transactionRepository, merchantRepository);
        Merchant merchant = new Merchant("Acme", "ACC-1");
        setId(merchant, 1L);
        CreateTransactionRequest request = new CreateTransactionRequest(1L, new BigDecimal("20.00"), "USD", "ext-9", PaymentType.CARD);
        Transaction concurrentlyInserted = new Transaction(merchant, new BigDecimal("20.00"), "USD", "ext-9");
        setId(concurrentlyInserted, 55L);

        when(merchantRepository.findById(1L)).thenReturn(Optional.of(merchant));
        when(transactionRepository.saveAndFlush(any(Transaction.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));
        when(transactionRepository.findByExternalReference("ext-9")).thenReturn(Optional.of(concurrentlyInserted));

        TransactionInserter.InsertOutcome outcome = inserter.insertNewTransaction(request);

        assertThat(outcome.created()).isFalse();
        assertThat(outcome.transaction().getId()).isEqualTo(55L);
    }

    @Test
    void insertNewTransaction_throwsNotFound_whenMerchantMissing() {
        inserter = new TransactionInserter(transactionRepository, merchantRepository);
        CreateTransactionRequest request = new CreateTransactionRequest(404L, new BigDecimal("20.00"), "USD", "ext-9", PaymentType.CARD);

        when(merchantRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inserter.insertNewTransaction(request))
                .isInstanceOf(ResourceNotFoundException.class);
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
