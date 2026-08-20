package com.payrecon.processing;

import com.payrecon.domain.PaymentType;
import com.payrecon.domain.Transaction;

/**
 * Strategy interface for the actual "call out to the payment rail" step.
 * Real implementations would call a card network or ACH/wire gateway; here
 * they're simulated, but the factory/strategy wiring around them is real.
 */
public interface PaymentProcessor {

    PaymentProcessingResult process(Transaction transaction);

    PaymentType supports();
}
