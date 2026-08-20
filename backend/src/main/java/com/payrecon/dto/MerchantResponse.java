package com.payrecon.dto;

import com.payrecon.domain.Merchant;

public record MerchantResponse(
        Long id,
        String name,
        String settlementAccount
) {
    public static MerchantResponse from(Merchant merchant) {
        return new MerchantResponse(merchant.getId(), merchant.getName(), merchant.getSettlementAccount());
    }
}
