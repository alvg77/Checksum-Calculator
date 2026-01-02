package com.ChecksumCalculator.exception;

public class ValidationException extends ChecksumCalculatorException {
    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}