package com.ChecksumCalculator;

import com.ChecksumCalculator.cli.CLIErrorHandler;
import com.ChecksumCalculator.cli.CLIHandler;

public class Main {
    static void main(String[] args) {
        try {
            CLIHandler CLIHandler = new CLIHandler(args);
            CLIHandler.execute();
        } catch (Exception e) {
            CLIErrorHandler.fatal(e.getMessage());
        }
    }
}
