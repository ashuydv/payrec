package com.payrecon.service.impl;

import com.payrecon.domain.LedgerEntry;
import com.payrecon.domain.LedgerSource;
import com.payrecon.domain.Merchant;
import com.payrecon.domain.ReconciliationStatus;
import com.payrecon.domain.Transaction;
import com.payrecon.dto.ReconciliationResponse;
import com.payrecon.exception.ResourceNotFoundException;
import com.payrecon.repository.LedgerEntryRepository;
import com.payrecon.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    private ReconciliationServiceImpl service;

    private Merchant merchant;
    private Transaction transaction;

    @BeforeEach
    void setUp() {
        service = new ReconciliationServiceImpl(transactionRepository, ledgerEntryRepository, new ReconciliationClassifier());
        merchant = new Merchant("Acme Co", "ACC-001");
        setId(merchant, 1L);
        transaction = new Transaction(merchant, new BigDecimal("100.00"), "USD", "ext-123");
        setId(transaction, 1L);
    }

    @Test
    void reconcile_throwsNotFound_whenTransactionMissing() {
        when(transactionRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reconcile(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void reconcile_returnsNoLedgerEntries_whenNoneRecorded() {
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));
        when(ledgerEntryRepository.findByTransactionReference("ext-123")).thenReturn(List.of());

        ReconciliationResponse response = service.reconcile(1L);

        assertThat(response.status()).isEqualTo(ReconciliationStatus.NO_LEDGER_ENTRIES);
        assertThat(response.ledgerEntries()).isEmpty();
    }

    @Test
    void reconcile_returnsMissingBankConfirmation_whenOnlyInternalEntryPresent() {
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));
        LedgerEntry internal = new LedgerEntry("ext-123", new BigDecimal("100.00"), LedgerSource.INTERNAL);
        when(ledgerEntryRepository.findByTransactionReference("ext-123")).thenReturn(List.of(internal));

        ReconciliationResponse response = service.reconcile(1L);

        assertThat(response.status()).isEqualTo(ReconciliationStatus.MISSING_BANK_CONFIRMATION);
    }

    @Test
    void reconcile_returnsMatched_whenInternalAndBankFeedAgreeWithTransactionAmount() {
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));
        LedgerEntry internal = new LedgerEntry("ext-123", new BigDecimal("100.00"), LedgerSource.INTERNAL);
        LedgerEntry bankFeed = new LedgerEntry("ext-123", new BigDecimal("100.0"), LedgerSource.BANK_FEED);
        when(ledgerEntryRepository.findByTransactionReference("ext-123")).thenReturn(List.of(internal, bankFeed));

        ReconciliationResponse response = service.reconcile(1L);

        assertThat(response.status()).isEqualTo(ReconciliationStatus.MATCHED);
        assertThat(response.ledgerEntries()).hasSize(2);
    }

    @Test
    void reconcile_returnsAmountMismatch_whenBankFeedAmountDisagrees() {
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));
        LedgerEntry internal = new LedgerEntry("ext-123", new BigDecimal("100.00"), LedgerSource.INTERNAL);
        LedgerEntry bankFeed = new LedgerEntry("ext-123", new BigDecimal("95.00"), LedgerSource.BANK_FEED);
        when(ledgerEntryRepository.findByTransactionReference("ext-123")).thenReturn(List.of(internal, bankFeed));

        ReconciliationResponse response = service.reconcile(1L);

        assertThat(response.status()).isEqualTo(ReconciliationStatus.AMOUNT_MISMATCH);
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
