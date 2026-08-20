package com.payrecon.dto;

/**
 * Wraps a created/idempotently-returned transaction along with whether this
 * call actually inserted a new row, so the controller can pick 201 vs 200
 * without an extra existence check (which would reopen the idempotency race
 * the service layer closes — see TransactionServiceImpl).
 */
public record CreateTransactionResult(TransactionResponse transaction, boolean created) {
}
