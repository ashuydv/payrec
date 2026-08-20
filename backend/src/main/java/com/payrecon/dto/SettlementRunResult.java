package com.payrecon.dto;

import java.io.Serializable;

/**
 * Serializable because SettlementTasklet stores it in a Spring Batch
 * ExecutionContext, which the JobRepository persists via Java
 * serialization by default.
 */
public record SettlementRunResult(
        String period,
        int transactionsConsidered,
        int transactionsSettled,
        int merchantsSettled,
        int merchantsSkippedFinalized,
        int merchantsSkippedConflict
) implements Serializable {
}
