package com.payrecon.dto;

import java.math.BigDecimal;

public record SettlementSummaryRow(
        Long merchantId,
        String merchantName,
        String period,
        BigDecimal totalAmount,
        String status
) {
}
