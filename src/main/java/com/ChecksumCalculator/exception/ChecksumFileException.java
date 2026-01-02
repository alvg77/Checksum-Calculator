package com.ChecksumCalculator.exception;

public class ChecksumFileException extends ChecksumCalculatorException {
    public ChecksumFileException(String message) {
        super(message);
    }

    public ChecksumFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
