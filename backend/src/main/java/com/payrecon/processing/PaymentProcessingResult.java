package com.payrecon.processing;

public record PaymentProcessingResult(boolean success, String message) {

    public static PaymentProcessingResult success(String message) {
        return new PaymentProcessingResult(true, message);
    }

    public static PaymentProcessingResult failure(String message) {
        return new PaymentProcessingResult(false, message);
    }
}
