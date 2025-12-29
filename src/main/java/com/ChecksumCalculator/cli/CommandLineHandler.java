package com.ChecksumCalculator.cli;

import org.apache.commons.cli.*;
import org.apache.commons.cli.help.HelpFormatter;

import java.io.IOException;
import java.util.Arrays;

public class CommandLineHandler {
    public static void handle(String[] args) {
        CommandLineParser parser = new DefaultParser();
        Options options = getOptions();

        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("h")) {
                HelpFormatter formatter = HelpFormatter.builder().setShowSince(false).get();
                formatter.printOptions(Arrays.asList(cmd.getOptions()));
                return;
            }

            parse(cmd);
        } catch (ParseException | IOException e) {
            throw new RuntimeException(e); // TODO:
        }
    }

    private static Options getOptions() {
        Options options = new Options();

        options.addOption("c", "checksums", true, "verification mode - specify checksums file");
        options.addOption("a", "algorithm", true, "algorithm to use (md5, sha1, sha256)");
        options.addOption("o", "output", true, "output file for results");
        options.addOption("f", "format", true, "output format (text, xml, json)");
        options.addOption("L", "follow-links", false, "follow symbolic links");
        options.addOption("r", "resume", true, "resume from state file");
        options.addOption("s", "state-file", true, "file to save state on pause");
        options.addOption("h", "help", false, "print this message");
        options.addOption("p", "path", true, "target file or directory (default: current directory)");

        return options;
    }

    private static void parse(CommandLine cmd) {
        // TODO: go through the rest of the options and build the required checksum class
    }
}
