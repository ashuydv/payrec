package com.payrecon.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * References Transaction.externalReference rather than a foreign key to
     * Transaction.id, since BANK_FEED entries arrive from an external system
     * that only knows the external reference, not our internal primary key.
     */
    @Column(name = "transaction_reference", nullable = false, length = 128)
    private String transactionReference;

    @Column(name = "recorded_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal recordedAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LedgerSource source;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected LedgerEntry() {
    }

    public LedgerEntry(String transactionReference, BigDecimal recordedAmount, LedgerSource source) {
        this.transactionReference = transactionReference;
        this.recordedAmount = recordedAmount;
        this.source = source;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public BigDecimal getRecordedAmount() {
        return recordedAmount;
    }

    public LedgerSource getSource() {
        return source;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
