package com.ChecksumCalculator.cli;

public class CLIErrorHandler {
    public enum ErrorType {
        IO;

        public int getStatusCode() {
            return switch (this) {
                case IO -> 1;
            };
        }
    }

    public static void fatal(String message, ErrorType err) {
        System.err.println(message);
        System.exit(err.getStatusCode());
    }

    public static void warn(String message) {
        IO.println("warning: " + message);
    }
}