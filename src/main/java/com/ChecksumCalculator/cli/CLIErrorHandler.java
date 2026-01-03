package com.ChecksumCalculator.cli;

public class CLIErrorHandler {
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_RESET = "\u001B[0m";

    public static void fatal(String message) {
        System.err.printf("Error: %s%n\n", message);
        System.exit(1);
    }

    public static void warn(String message) {
        System.err.printf("%sWarning: %s%s\n", ANSI_YELLOW, message, ANSI_RESET);
    }
}
