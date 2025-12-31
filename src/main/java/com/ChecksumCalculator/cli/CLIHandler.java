package com.ChecksumCalculator.cli;

import com.ChecksumCalculator.core.calculator.ChecksumCalculator;
import com.ChecksumCalculator.core.calculator.MD5Calculator;
import com.ChecksumCalculator.core.calculator.SHA1Calculator;
import com.ChecksumCalculator.core.calculator.SHA512Calculator;
import com.ChecksumCalculator.core.verification.HashComparator;
import com.ChecksumCalculator.filesystem.builder.FSBuilder;
import com.ChecksumCalculator.filesystem.builder.FollowLinksFSBuilder;
import com.ChecksumCalculator.filesystem.builder.StandardFSBuilder;
import com.ChecksumCalculator.filesystem.node.FSNode;
import com.ChecksumCalculator.filesystem.visitor.HashStreamWriter;
import com.ChecksumCalculator.filesystem.visitor.ReportWriter;
import com.ChecksumCalculator.io.reader.ChecksumReader;
import com.ChecksumCalculator.io.reader.JSONChecksumReader;
import com.ChecksumCalculator.io.reader.TextChecksumReader;
import com.ChecksumCalculator.io.serializer.ComparisonResultSerializer;
import com.ChecksumCalculator.io.serializer.checksum.ChecksumSerializer;
import com.ChecksumCalculator.io.serializer.checksum.JSONChecksumSerializer;
import com.ChecksumCalculator.io.serializer.checksum.TextChecksumSerializer;
import com.ChecksumCalculator.progress.ProgressReporter;
import org.apache.commons.cli.*;
import org.apache.commons.cli.help.HelpFormatter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CLIHandler {
    private static final Set<String> VALID_ALGORITHMS = Set.of("md5", "sha1", "sha512");
    private static final Set<String> VALID_FORMATS = Set.of("text", "json");

    private final CommandLine cmd;
    private final Options options;
    private Mode mode;
    private final List<String> validationErrors = new ArrayList<>();

    public CLIHandler(String[] args) {
        options = getOptions();
        CommandLineParser parser = new DefaultParser();

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Error: " + e.getMessage());
            printHelp();
            System.exit(1);
            throw new RuntimeException(e);
        }
    }

    public void execute() {
        if (cmd.hasOption("help")) {
            printHelp();
            return;
        }

        determineMode();
        validateOptions();

        if (!validationErrors.isEmpty()) {
            printValidationErrors();
            System.exit(1);
        }

        switch (mode) {
            case VERIFY -> executeVerification();
            case RESUME -> executeResume();
            case CALCULATE -> executeCalculation();
        }
    }

    private void determineMode() {
        boolean hasChecksums = cmd.hasOption("checksums");
        boolean hasResume = cmd.hasOption("resume");

        if (hasChecksums && hasResume) {
            validationErrors.add("Cannot use both --checksums and --resume at the same time");
            mode = Mode.CALCULATE;
            return;
        }

        if (hasChecksums) {
            mode = Mode.VERIFY;
        } else if (hasResume) {
            mode = Mode.RESUME;
        } else {
            mode = Mode.CALCULATE;
        }
    }

    private void validateOptions() {
        validateAlgorithm();
        validateFormat();
        validatePath();

        switch (mode) {
            case VERIFY -> validateVerificationMode();
            case RESUME -> validateResumeMode();
            case CALCULATE -> validateCalculationMode();
        }
    }

    private void validateAlgorithm() {
        if (cmd.hasOption("algorithm")) {
            String algo = cmd.getOptionValue("algorithm");
            if (isBlank(algo)) {
                validationErrors.add("Algorithm cannot be empty");
                return;
            }
            if (!VALID_ALGORITHMS.contains(algo.toLowerCase())) {
                validationErrors.add(String.format(
                        "Unknown algorithm: '%s'. Valid options are: %s",
                        algo, String.join(", ", VALID_ALGORITHMS)
                ));
            }
        }
    }

    private void validateFormat() {
        if (cmd.hasOption("format")) {
            String format = cmd.getOptionValue("format");
            if (isBlank(format)) {
                validationErrors.add("Format cannot be empty");
                return;
            }
            if (!VALID_FORMATS.contains(format.toLowerCase())) {
                validationErrors.add(String.format(
                        "Unknown format: '%s'. Valid options are: %s",
                        format, String.join(", ", VALID_FORMATS)
                ));
            }
        }
    }

    private void validatePath() {
        String path = cmd.hasOption("path") ? cmd.getOptionValue("path") : ".";

        if (isBlank(path)) {
            validationErrors.add("Path cannot be empty");
            return;
        }

        File targetPath = new File(path);
        if (!targetPath.exists()) {
            validationErrors.add("Path does not exist: " + path);
        } else if (!targetPath.canRead()) {
            validationErrors.add("Cannot read path (permission denied): " + path);
        }
    }

    private void validateVerificationMode() {
        String checksumsPath = cmd.getOptionValue("checksums");

        if (isBlank(checksumsPath)) {
            validationErrors.add("Checksums file path cannot be empty");
            return;
        }

        File checksumsFile = new File(checksumsPath);
        if (!checksumsFile.exists()) {
            validationErrors.add("Checksums file does not exist: " + checksumsPath);
        } else if (!checksumsFile.isFile()) {
            validationErrors.add("Checksums path is not a file: " + checksumsPath);
        } else if (!checksumsFile.canRead()) {
            validationErrors.add("Cannot read checksums file (permission denied): " + checksumsPath);
        }

        validateFormatExtensionConsistency(checksumsPath);
    }

    private void validateResumeMode() {
        String resumePath = cmd.getOptionValue("resume");

        if (isBlank(resumePath)) {
            validationErrors.add("Resume state file path cannot be empty");
            return;
        }

        File resumeFile = new File(resumePath);
        if (!resumeFile.exists()) {
            validationErrors.add("State file does not exist: " + resumePath);
        } else if (!resumeFile.isFile()) {
            validationErrors.add("State file path is not a file: " + resumePath);
        } else if (!resumeFile.canRead()) {
            validationErrors.add("Cannot read state file (permission denied): " + resumePath);
        }
    }

    private void validateCalculationMode() {
        File outputFile = parseOutput();
        String outputPath = outputFile.getAbsolutePath();

        if (outputFile.exists()) {
            if (!outputFile.isFile()) {
                validationErrors.add("Output path exists but is not a file: " + outputPath);
                return;
            }
            if (!outputFile.canWrite()) {
                validationErrors.add("Cannot write to existing output file (permission denied): " + outputPath);
                return;
            }

            IO.println("Warning: Output file already exists and will be overwritten: " + outputPath);
        } else {

            File parentDir = outputFile.getParentFile();
            if (parentDir == null) {
                parentDir = new File(".");
            }

            if (!parentDir.exists()) {
                validationErrors.add("Output directory does not exist: " + parentDir.getAbsolutePath());
            } else if (!parentDir.canWrite()) {
                validationErrors.add("Cannot write to output directory (permission denied): " + parentDir.getAbsolutePath());
            }
        }


        validateFormatExtensionConsistency(outputFile.getName());
    }

    private void validateFormatExtensionConsistency(String fileName) {
        if (!cmd.hasOption("format")) {
            return;
        }

        String format = cmd.getOptionValue("format").toLowerCase();
        String extension = getFileExtension(fileName).toLowerCase();

        boolean mismatch = false;
        String expectedExtension = "";

        if (format.equals("json") && !extension.equals("json")) {
            mismatch = true;
            expectedExtension = ".json";
        } else if (format.equals("text") && extension.equals("json")) {
            mismatch = true;
            expectedExtension = ".txt";
        }

        if (mismatch) {
            IO.println(String.format(
                    "Warning: Format '%s' doesn't match file extension '%s'. Consider using %s extension.",
                    format, extension.isEmpty() ? "(none)" : "." + extension, expectedExtension
            ));
        }
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1 || lastDot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDot + 1);
    }

    private void printValidationErrors() {
        System.err.println("Validation failed with " + validationErrors.size() + " error(s):");
        System.err.println();
        for (int i = 0; i < validationErrors.size(); i++) {
            System.err.println("  " + (i + 1) + ". " + validationErrors.get(i));
        }
        System.err.println();
        System.err.println("Use --help for usage information.");
    }

    private void executeVerification() {
        ChecksumCalculator calculator = parseAlgorithm();
        FSBuilder fsBuilder = new StandardFSBuilder();
        String path = parsePath();
        File inputFile = parseChecksumsFile();
        Format format = parseFormat(inputFile.getName());

        ChecksumReader cr = switch (format) {
            case JSON -> new JSONChecksumReader(inputFile.getPath());
            case TEXT -> new TextChecksumReader(inputFile.getPath());
        };

        FSNode root = fsBuilder.build(path);

        report(root);

        HashComparator hc = new HashComparator(cr, calculator);
        hc.addObserver(new ProgressReporter(root.getSizeInBytes()));
        ComparisonResultSerializer crs = new ComparisonResultSerializer(System.out);
        crs.write(hc.compare(root));
    }

    private void executeResume() {
        System.err.println("Resume functionality not yet implemented.");
        System.exit(1);
    }

    private void executeCalculation() {
        ChecksumCalculator calculator = parseAlgorithm();
        FSBuilder fsBuilder = parseFollowLinks();
        String path = parsePath();
        File outputFile = parseOutput();
        Format format = parseFormat(outputFile.getName());

        FSNode root = fsBuilder.build(path);

        if (root.getSizeInBytes() == 0) {
            IO.println("Warning: No files found to process at path: " + path);
        }

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            ChecksumSerializer serializer = null;

            try {
                serializer = switch (format) {
                    case TEXT -> new TextChecksumSerializer(fos);
                    case JSON -> new JSONChecksumSerializer(fos);
                };
            } catch (IOException e) {
                CLIErrorHandler.fatal(e.getMessage(), CLIErrorHandler.ErrorType.IO);
            }

            assert serializer != null;

            report(root);

            HashStreamWriter hsw = new HashStreamWriter(serializer, calculator);
            hsw.addObserver(new ProgressReporter(root.getSizeInBytes()));
            hsw.write(root);
            IO.println("\nDone! Results written to: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            CLIErrorHandler.fatal(e.getMessage(), CLIErrorHandler.ErrorType.IO);
        }
    }

    private ChecksumCalculator parseAlgorithm() {
        if (cmd.hasOption("algorithm")) {
            return switch (cmd.getOptionValue("algorithm").toLowerCase()) {
                case "sha1" -> new SHA1Calculator();
                case "sha512" -> new SHA512Calculator();
                default -> new MD5Calculator();
            };
        }
        return new MD5Calculator();
    }

    private File parseOutput() {
        if (cmd.hasOption("output")) {
            return new File(cmd.getOptionValue("output"));
        }
        return new File("checksums.txt");
    }

    private Format parseFormat(String fileName) {
        if (cmd.hasOption("format")) {
            return switch (cmd.getOptionValue("format").toLowerCase()) {
                case "json" -> Format.JSON;
                default -> Format.TEXT;
            };
        }

        if (fileName.toLowerCase().endsWith(".json")) {
            return Format.JSON;
        }

        return Format.TEXT;
    }

    private FSBuilder parseFollowLinks() {
        if (cmd.hasOption("follow-links")) {
            return new FollowLinksFSBuilder();
        }
        return new StandardFSBuilder();
    }

    private File parseStateFile() {
        if (cmd.hasOption("state-file")) {
            return new File(cmd.getOptionValue("state-file"));
        }
        return new File("checksums.state");
    }

    private String parsePath() {
        if (cmd.hasOption("path")) {
            return cmd.getOptionValue("path");
        }
        return ".";
    }

    private File parseChecksumsFile() {
        if (cmd.hasOption("checksums")) {
            return new File(cmd.getOptionValue("checksums"));
        }
        return new File("checksums.txt");
    }

    private void printHelp() {
        System.out.println("Checksum Calculator - Calculate and verify file checksums");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  Calculate: checksum [-p PATH] [-a ALGORITHM] [-o OUTPUT] [-f FORMAT] [-L]");
        System.out.println("  Verify:    checksum -c CHECKSUMS_FILE [-p PATH] [-a ALGORITHM]");
        System.out.println("  Resume:    checksum -r STATE_FILE");
        System.out.println();

        HelpFormatter formatter = HelpFormatter.builder().setShowSince(false).get();
        try {
            formatter.printOptions(options);
        } catch (IOException e) {
            CLIErrorHandler.fatal(e.getMessage(), CLIErrorHandler.ErrorType.IO);
        }

        System.out.println();
        System.out.println("Examples:");
        System.out.println("  checksum -p /home/user/documents -a sha256 -o hashes.json");
        System.out.println("  checksum -c hashes.json -p /home/user/documents");
        System.out.println("  checksum -p . -L -f text");
    }

    private static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    private void report(FSNode root) {
        ReportWriter rw = new ReportWriter(System.out);
        root.accept(rw);

        IO.println("\nPress any key to continue...");
        IO.readln();
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