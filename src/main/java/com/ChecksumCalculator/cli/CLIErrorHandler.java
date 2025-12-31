package com.ChecksumCalculator.cli;

public class CLIErrorHandler {
    public static void fatal(String message) {
        System.err.println(message);
        System.exit(1);
    }

    public static void warn(String message) {
        IO.println("warning: " + message);
    }
}

//Error-Handling
//When implementing the program, proactively think about the possible errors that can
//occur and how to handle them. For example, what should happen if the user specifies a
//non-existent file or directory? Or if the user supplies the --checksums switch, but specifies a file that is not a checksum file? Or if the user specifies a target that cannot be read?
//The program must graciously handle all such cases and provide meaningful error messages to the user.
//If the error is critical and the program cannot continue, it must exit with a non-zero exit
//code

//// Simplified approach that covers most cases well
//public class CLI {
//    private static boolean verbose = false;
//    private static boolean quiet = false;
//
//    public static void fatal(String message) {
//        System.err.println("error: " + message);
//        System.exit(1);
//    }
//
//    public static void fatal(String message, Throwable cause) {
//        System.err.println("error: " + message);
//        if (verbose) {
//            cause.printStackTrace(System.err);
//        }
//        System.exit(1);
//    }
//
//    public static void warn(String message) {
//        if (!quiet) {
//            System.err.println("warning: " + message);
//        }
//    }
//
//    public static void info(String message) {
//        if (!quiet) {
//            System.out.println(message);
//        }
//    }
//
//    public static void debug(String message) {
//        if (verbose) {
//            System.err.println("debug: " + message);
//        }
//    }
//}