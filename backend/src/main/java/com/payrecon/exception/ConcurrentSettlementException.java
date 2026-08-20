package com.payrecon.exception;

public class ConcurrentSettlementException extends RuntimeException {

    public ConcurrentSettlementException(String message, Throwable cause) {
        super(message, cause);
    }
}
