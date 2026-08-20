package com.payrecon.dto;

import com.payrecon.domain.Merchant;
import com.payrecon.domain.Settlement;
import com.payrecon.domain.SettlementStatus;

import java.math.BigDecimal;

public record SettlementResponse(
        Long id,
        Long merchantId,
        String merchantName,
        String period,
        BigDecimal totalAmount,
        SettlementStatus status,
        Long version
) {
    /**
     * Requires an active Hibernate session, since it dereferences the lazy
     * Settlement.merchant association. Callers holding a Settlement that
     * outlived its transaction (e.g. returned from a REQUIRES_NEW method
     * that already committed) must use {@link #from(Settlement, Merchant)}
     * with an already-loaded Merchant instead.
     */
    public static SettlementResponse from(Settlement s) {
        return from(s, s.getMerchant());
    }

    public static SettlementResponse from(Settlement s, Merchant merchant) {
        return new SettlementResponse(
                s.getId(),
                merchant.getId(),
                merchant.getName(),
                s.getPeriod(),
                s.getTotalAmount(),
                s.getStatus(),
                s.getVersion()
        );
    }
}
