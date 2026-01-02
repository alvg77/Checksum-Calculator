package com.ChecksumCalculator;

import com.ChecksumCalculator.cli.CLIErrorHandler;
import com.ChecksumCalculator.cli.CLIHandler;
import com.ChecksumCalculator.exception.ChecksumCalculatorException;
import com.ChecksumCalculator.exception.FileSystemException;
import com.ChecksumCalculator.exception.ValidationException;

public class Main {
    static void main(String[] args) {
        try {
            CLIHandler handler = new CLIHandler(args);
            handler.execute();
        } catch (ValidationException e) {
            CLIErrorHandler.fatal("Invalid input: " + e.getMessage());
            System.exit(1);
        } catch (FileSystemException e) {
            CLIErrorHandler.fatal(String.format("File system error: %s", e.getMessage()));
            System.exit(2);
        } catch (ChecksumCalculatorException e) {
            CLIErrorHandler.fatal("Error: " + e.getMessage());
            System.exit(3);
        } catch (Exception e) {
            CLIErrorHandler.fatal("Unexpected error: " + e.getMessage());
            System.exit(99);
        }
    }
}
