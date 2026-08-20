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
import jakarta.persistence.Version;

import java.math.BigDecimal;

@Entity
@Table(
        name = "settlements",
        uniqueConstraints = @UniqueConstraint(name = "uk_settlement_merchant_period", columnNames = {"merchant_id", "period"})
)
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    /** Settlement period, e.g. "2026-08-19" for a daily settlement. */
    @Column(nullable = false, length = 20)
    private String period;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SettlementStatus status;

    /**
     * Optimistic lock to prevent two concurrent settlement runs for the same
     * merchant/period from double-counting transactions (see Phase 4).
     */
    @Version
    private Long version;

    protected Settlement() {
    }

    public Settlement(Merchant merchant, String period, BigDecimal totalAmount) {
        this.merchant = merchant;
        this.period = period;
        this.totalAmount = totalAmount;
        this.status = SettlementStatus.OPEN;
    }

    public Long getId() {
        return id;
    }

    public Merchant getMerchant() {
        return merchant;
    }

    public String getPeriod() {
        return period;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public SettlementStatus getStatus() {
        return status;
    }

    public void setStatus(SettlementStatus status) {
        this.status = status;
    }

    public Long getVersion() {
        return version;
    }
}
