package com.payrecon.processing.strategy;

import com.payrecon.domain.PaymentType;
import com.payrecon.domain.Transaction;
import com.payrecon.processing.PaymentProcessingResult;
import com.payrecon.processing.PaymentProcessor;
import org.springframework.stereotype.Component;

/**
 * Simulates an ACH/wire transfer initiation. Real bank transfers settle
 * asynchronously (T+1/T+2), which is exactly the kind of gap the nightly
 * reconciliation job (Phase 3) exists to catch; here it always succeeds to
 * keep the demo deterministic.
 */
@Component
public class BankTransferPaymentProcessor implements PaymentProcessor {

    @Override
    public PaymentProcessingResult process(Transaction transaction) {
        return PaymentProcessingResult.success(
                "Bank transfer initiated for " + transaction.getExternalReference());
    }

    @Override
    public PaymentType supports() {
        return PaymentType.BANK_TRANSFER;
    }
}
