package com.payrecon.processing.strategy;

import com.payrecon.domain.PaymentType;
import com.payrecon.domain.Transaction;
import com.payrecon.processing.PaymentProcessingResult;
import com.payrecon.processing.PaymentProcessor;
import org.springframework.stereotype.Component;

/**
 * Simulates a card-network authorization + capture. In production this would
 * call out to a processor (Stripe, Adyen, etc.); here it always succeeds to
 * keep the demo deterministic.
 */
@Component
public class CardPaymentProcessor implements PaymentProcessor {

    @Override
    public PaymentProcessingResult process(Transaction transaction) {
        return PaymentProcessingResult.success(
                "Card payment authorized and captured for " + transaction.getExternalReference());
    }

    @Override
    public PaymentType supports() {
        return PaymentType.CARD;
    }
}
