package com.ChecksumCalculator.cli;

import com.ChecksumCalculator.exception.ValidationException;
import org.apache.commons.cli.*;
import org.apache.commons.cli.help.HelpFormatter;

import java.io.IOException;

public class CLIHandler {
    private final CommandLine cmd;
    private final Options options;

    public CLIHandler(String[] args) {
        options = getOptions();
        CommandLineParser parser = new DefaultParser();

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            throw new ValidationException(e.getMessage());
        }
    }

    public void execute() {
        if (cmd.hasOption("help")) {
            printHelp();
            return;
        }

        CLIConfig config = new CLIConfig(cmd);
        config.validate();

        CommandExecutor executor = new CommandExecutor();

        switch (config.getMode()) {
            case VERIFY -> executor.verify(config);
            case RESUME -> executor.resume(config);
            case CALCULATE -> executor.calculate(config);
        }
    }

    private void printHelp() {
        System.out.println("Checksum Calculator - Calculate and verify file checksums");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  Calculate: checksum [-p PATH] [-a ALGORITHM] [-o OUTPUT] [-f FORMAT] [-s STATE_FILE] [-L]");
        System.out.println("  Verify:    checksum -c CHECKSUMS_FILE [-p PATH] [-a ALGORITHM]");
        System.out.println("  Resume:    checksum -r STATE_FILE");
        System.out.println();

        HelpFormatter formatter = HelpFormatter.builder().setShowSince(false).get();
        try {
            formatter.printOptions(options);
        } catch (IOException e) {
            CLIErrorHandler.fatal(e.getMessage());
        }

        System.out.println();
        System.out.println("Examples:");
        System.out.println("  checksum -p /home/user/documents -a sha256 -o hashes.json");
        System.out.println("  checksum -c hashes.json -p /home/user/documents");
        System.out.println("  checksum -p . -L -f text");
    }

    private static Options getOptions() {
        Options options = new Options();

        options.addOption("h", "help", false, "print this message");
        options.addOption("p", "path", true, "target file or directory (default: current directory)");
        options.addOption("L", "follow-links", false, "follow symbolic links");
        options.addOption("a", "algorithm", true, "algorithm to use (md5, sha1, sha512)");
        options.addOption("o", "output", true, "output file for results (default: checksums.txt)");
        options.addOption("f", "format", true, "output format (text, json) - inferred from extension if not specified");
        options.addOption("c", "checksums", true, "verification mode - specify checksums file to verify against");
        options.addOption("s", "state-file", true, "file to save state on pause (default: checksums.state)");
        options.addOption("r", "resume", true, "resume calculation from state file");

        return options;
    }
}