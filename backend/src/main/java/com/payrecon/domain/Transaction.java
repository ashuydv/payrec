package com.payrecon.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "transactions",
        uniqueConstraints = @UniqueConstraint(name = "uk_transaction_external_reference", columnNames = "external_reference")
)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 20)
    private PaymentType paymentType;

    /**
     * Natural key supplied by the upstream feed/caller. The unique constraint on
     * this column is what makes transaction creation idempotent (see
     * TransactionServiceImpl).
     */
    @Column(name = "external_reference", nullable = false, length = 128)
    private String externalReference;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    protected Transaction() {
    }

    public Transaction(Merchant merchant, BigDecimal amount, String currency, String externalReference) {
        this(merchant, amount, currency, externalReference, PaymentType.CARD);
    }

    public Transaction(Merchant merchant, BigDecimal amount, String currency, String externalReference, PaymentType paymentType) {
        this.merchant = merchant;
        this.amount = amount;
        this.currency = currency;
        this.externalReference = externalReference;
        this.paymentType = paymentType;
        this.status = TransactionStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Merchant getMerchant() {
        return merchant;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public PaymentType getPaymentType() {
        return paymentType;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
        if (status == TransactionStatus.PROCESSED) {
            this.processedAt = Instant.now();
        }
    }

    public String getExternalReference() {
        return externalReference;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public Instant getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(Instant nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }
}
