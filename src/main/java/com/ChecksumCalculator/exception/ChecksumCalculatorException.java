package com.ChecksumCalculator.exception;

public class ChecksumCalculatorException extends RuntimeException {
    public ChecksumCalculatorException(String message) {
        super(message);
    }

    public ChecksumCalculatorException(String message, Throwable cause) {
        super(message, cause);
    }
}