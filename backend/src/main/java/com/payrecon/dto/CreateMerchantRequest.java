package com.payrecon.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateMerchantRequest(

        @NotBlank(message = "name is required")
        String name,

        @NotBlank(message = "settlementAccount is required")
        String settlementAccount
) {
}
