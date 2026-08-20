package com.payrecon.dto;

import com.payrecon.domain.SettlementStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateSettlementStatusRequest(

        @NotNull(message = "status is required")
        SettlementStatus status
) {
}
