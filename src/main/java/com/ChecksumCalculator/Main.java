package com.ChecksumCalculator;

import com.ChecksumCalculator.cli.CLIHandler;

public class Main {
    static void main(String[] args) {
        CLIHandler CLIHandler = new CLIHandler(args);
        CLIHandler.execute();
    }
}
