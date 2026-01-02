package com.ChecksumCalculator.cli;

import java.util.List;

public class CLIErrorHandler {
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_RESET = "\u001B[0m";

    public static void fatal(String message) {
        System.err.printf("Error: %s%n\n", message);
        System.exit(1);
    }

    public static void fatal(String message, Runnable runnable) {
        System.err.printf("Error: %s%n\n", message);
        runnable.run();
        System.exit(1);
    }

    public static void fatal(List<String> messages) {
        printFatalList(messages);
        System.exit(1);
    }

    public static void fatal(List<String> messages, Runnable runnable) {
        printFatalList(messages);
        runnable.run();
        System.exit(1);
    }

    public static void warn(String message) {
        System.err.printf("%sWarning: %s%s\n", ANSI_YELLOW, message, ANSI_RESET);
    }

    private static void printFatalList(List<String> messages) {
        if (messages == null || messages.isEmpty()) {
            fatal("Unknown error occurred");
            return;
        }

        System.err.println("Error" + messages.getFirst());
        for (int i = 1; i < messages.size(); i++) {
            System.err.println("  " + messages.get(i));
        }
    }
}
