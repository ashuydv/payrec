package com.payrecon.dto;

import com.payrecon.domain.TransactionStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTransactionStatusRequest(

        @NotNull(message = "status is required")
        TransactionStatus status
) {
}
