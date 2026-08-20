package com.payrecon.processing;

import com.payrecon.domain.PaymentType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory pattern: Spring injects every PaymentProcessor bean in the
 * context, and this factory indexes them by the PaymentType they support so
 * callers can ask for "the processor for CARD" without knowing the concrete
 * class. Adding a new payment rail is then just adding a new
 * {@code @Component} implementing PaymentProcessor — no changes needed here.
 */
@Component
public class PaymentProcessorFactory {

    private final Map<PaymentType, PaymentProcessor> processorsByType;

    public PaymentProcessorFactory(List<PaymentProcessor> processors) {
        this.processorsByType = processors.stream()
                .collect(Collectors.toUnmodifiableMap(PaymentProcessor::supports, Function.identity()));
    }

    public PaymentProcessor getProcessor(PaymentType type) {
        PaymentProcessor processor = processorsByType.get(type);
        if (processor == null) {
            throw new IllegalArgumentException("No PaymentProcessor registered for type: " + type);
        }
        return processor;
    }
}
