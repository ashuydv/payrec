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
import java.time.LocalDate;

@Entity
@Table(name = "reconciliation_reports")
public class ReconciliationReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_date", nullable = false)
    private LocalDate runDate;

    @Column(name = "transaction_reference", nullable = false, length = 128)
    private String transactionReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReconciliationOutcome outcome;

    @Column(name = "internal_amount", precision = 19, scale = 4)
    private BigDecimal internalAmount;

    @Column(name = "bank_amount", precision = 19, scale = 4)
    private BigDecimal bankAmount;

    @Column(length = 500)
    private String discrepancy;

    /** True until an ops user has reviewed a MISMATCHED/MISSING entry. */
    @Column(name = "needs_review", nullable = false)
    private boolean needsReview;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ReconciliationReport() {
    }

    public ReconciliationReport(LocalDate runDate, String transactionReference, ReconciliationOutcome outcome,
                                 BigDecimal internalAmount, BigDecimal bankAmount, String discrepancy) {
        this.runDate = runDate;
        this.transactionReference = transactionReference;
        this.outcome = outcome;
        this.internalAmount = internalAmount;
        this.bankAmount = bankAmount;
        this.discrepancy = discrepancy;
        this.needsReview = outcome != ReconciliationOutcome.MATCHED;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public LocalDate getRunDate() {
        return runDate;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public ReconciliationOutcome getOutcome() {
        return outcome;
    }

    public BigDecimal getInternalAmount() {
        return internalAmount;
    }

    public BigDecimal getBankAmount() {
        return bankAmount;
    }

    public String getDiscrepancy() {
        return discrepancy;
    }

    public boolean isNeedsReview() {
        return needsReview;
    }

    public void setNeedsReview(boolean needsReview) {
        this.needsReview = needsReview;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
