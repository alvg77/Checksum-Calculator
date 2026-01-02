package com.ChecksumCalculator.exception;

public class FileSystemException extends ChecksumCalculatorException {
    public FileSystemException(String message) {
        super(message);
    }

    public FileSystemException(String message, Throwable cause) {
        super(message, cause);
    }
}
