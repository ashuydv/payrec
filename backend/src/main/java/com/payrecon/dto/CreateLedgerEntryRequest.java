package com.payrecon.dto;

import com.payrecon.domain.LedgerSource;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateLedgerEntryRequest(

        @NotBlank(message = "transactionReference is required")
        String transactionReference,

        @NotNull(message = "recordedAmount is required")
        @DecimalMin(value = "0.01", message = "recordedAmount must be greater than zero")
        BigDecimal recordedAmount,

        @NotNull(message = "source is required")
        LedgerSource source
) {
}
