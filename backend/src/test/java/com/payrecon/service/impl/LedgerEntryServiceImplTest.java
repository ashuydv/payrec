package com.payrecon.service.impl;

import com.payrecon.domain.LedgerEntry;
import com.payrecon.domain.LedgerSource;
import com.payrecon.dto.CreateLedgerEntryRequest;
import com.payrecon.dto.LedgerEntryResponse;
import com.payrecon.repository.LedgerEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LedgerEntryServiceImplTest {

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    private LedgerEntryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new LedgerEntryServiceImpl(ledgerEntryRepository);
    }

    @Test
    void ingest_savesAndReturnsResponse() {
        CreateLedgerEntryRequest request =
                new CreateLedgerEntryRequest("ext-123", new BigDecimal("100.00"), LedgerSource.BANK_FEED);
        LedgerEntry saved = new LedgerEntry(request.transactionReference(), request.recordedAmount(), request.source());
        setId(saved, 1L);
        when(ledgerEntryRepository.save(ArgumentMatchers.any(LedgerEntry.class))).thenReturn(saved);

        LedgerEntryResponse response = service.ingest(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.transactionReference()).isEqualTo("ext-123");
        assertThat(response.source()).isEqualTo(LedgerSource.BANK_FEED);
    }

    @Test
    void listByTransactionReference_returnsAllEntriesForReference() {
        LedgerEntry internal = new LedgerEntry("ext-123", new BigDecimal("100.00"), LedgerSource.INTERNAL);
        setId(internal, 1L);
        LedgerEntry bankFeed = new LedgerEntry("ext-123", new BigDecimal("100.00"), LedgerSource.BANK_FEED);
        setId(bankFeed, 2L);
        when(ledgerEntryRepository.findByTransactionReference("ext-123")).thenReturn(List.of(internal, bankFeed));

        List<LedgerEntryResponse> responses = service.listByTransactionReference("ext-123");

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(LedgerEntryResponse::source)
                .containsExactlyInAnyOrder(LedgerSource.INTERNAL, LedgerSource.BANK_FEED);
    }

    @Test
    void listByTransactionReference_returnsEmptyList_whenNoEntries() {
        when(ledgerEntryRepository.findByTransactionReference("ext-unknown")).thenReturn(List.of());

        assertThat(service.listByTransactionReference("ext-unknown")).isEmpty();
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
