package com.ChecksumCalculator.core.verification;

public enum Status {
    OK,
    MODIFIED,
    NEW,
    REMOVED;

    public String toString() {
        return switch (this) {
            case OK -> "OK";
            case MODIFIED -> "MODIFIED";
            case NEW -> "NEW";
            case REMOVED -> "REMOVED";
        };
    }
}
