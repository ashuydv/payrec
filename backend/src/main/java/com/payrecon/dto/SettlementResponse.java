package com.payrecon.dto;

import com.payrecon.domain.Settlement;
import com.payrecon.domain.SettlementStatus;

import java.math.BigDecimal;

public record SettlementResponse(
        Long id,
        Long merchantId,
        String merchantName,
        String period,
        BigDecimal totalAmount,
        SettlementStatus status
) {
    public static SettlementResponse from(Settlement settlement) {
        return new SettlementResponse(
                settlement.getId(),
                settlement.getMerchant().getId(),
                settlement.getMerchant().getName(),
                settlement.getPeriod(),
                settlement.getTotalAmount(),
                settlement.getStatus()
        );
    }
}
