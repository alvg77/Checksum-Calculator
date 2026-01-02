package com.ChecksumCalculator.cli;

import com.ChecksumCalculator.calculator.*;
import com.ChecksumCalculator.exception.ValidationException;
import com.ChecksumCalculator.filesystem.visitor.HashComparator;
import com.ChecksumCalculator.filesystem.visitor.Status;
import com.ChecksumCalculator.filesystem.builder.FSBuilder;
import com.ChecksumCalculator.filesystem.builder.FSDirector;
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
import com.ChecksumCalculator.state.PauseController;
import com.ChecksumCalculator.state.StateManager;
import com.ChecksumCalculator.state.memento.CalculationStateMemento;
import com.ChecksumCalculator.state.memento.VerificationStateMemento;
import org.apache.commons.cli.*;
import org.apache.commons.cli.help.HelpFormatter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;

public class CLIHandler {
    private static final Set<String> VALID_ALGORITHMS = Set.of("md5", "sha1", "sha512");
    private static final Set<String> VALID_FORMATS = Set.of("text", "json");

    private final CommandLine cmd;
    private final Options options;
    private Mode mode;

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

        determineMode();
        validateOptions();

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
            throw new ValidationException("Cannot have both --checksums and --resume at the same time");
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
            String algorithm = cmd.getOptionValue("algorithm");
            if (isBlank(algorithm)) {
                throw new ValidationException("Algorithm cannot be empty.");
            }
            if (!VALID_ALGORITHMS.contains(algorithm.toLowerCase())) {
                throw new ValidationException(String.format(
                        "Unknown algorithm: '%s'. Valid options are: %s",
                        algorithm, String.join(", ", VALID_ALGORITHMS)
                ));
            }
        }
    }

    private void validateFormat() {
        if (cmd.hasOption("format")) {
            String format = cmd.getOptionValue("format");
            if (isBlank(format)) {
                throw new ValidationException("Format cannot be empty");
            }
            if (!VALID_FORMATS.contains(format.toLowerCase())) {
                throw new ValidationException(String.format(
                        "Unknown format: '%s'. Valid options are: %s",
                        format, String.join(", ", VALID_FORMATS)
                ));
            }
        }
    }

    private void validatePath() {
        String path = cmd.hasOption("path") ? cmd.getOptionValue("path") : ".";

        if (isBlank(path)) {
            throw new ValidationException("Path cannot be empty");
        }

        File targetPath = new File(path);
        if (!targetPath.exists()) {
            throw new ValidationException(String.format("Path does not exist: %s", path));
        } else if (!targetPath.canRead()) {
            throw new ValidationException(String.format("Cannot read path: %s", path));
        }
    }

    private void validateVerificationMode() {
        String checksumsPath = cmd.getOptionValue("checksums");

        if (isBlank(checksumsPath)) {
            throw new ValidationException("Checksums file path cannot be empty");
        }

        File checksumsFile = new File(checksumsPath);
        if (!checksumsFile.exists()) {
            throw new ValidationException(String.format("Checksums file does not exist: %s", checksumsPath));
        } else if (!checksumsFile.isFile()) {
            throw new ValidationException(String.format("Checksums path is not a file: %s", checksumsPath));
        } else if (!checksumsFile.canRead()) {
            throw new ValidationException(String.format("Cannot read checksums file: %s", checksumsPath));
        }

        validateFormatExtensionConsistency(checksumsPath);
    }

    private void validateResumeMode() {
        String resumePath = cmd.getOptionValue("resume");

        if (isBlank(resumePath)) {
            throw new ValidationException("Resume state file path cannot be empty");
        }

        File resumeFile = new File(resumePath);
        if (!resumeFile.exists()) {
            throw new ValidationException(String.format("State file does not exist: %s", resumePath));
        } else if (!resumeFile.isFile()) {
            throw new ValidationException(String.format("State file path is not a file: %s", resumePath));
        } else if (!resumeFile.canRead()) {
            throw new ValidationException(String.format("Cannot read state file: %s", resumePath));
        }
    }

    private void validateCalculationMode() {
        File outputFile = parseOutput();
        String outputPath = outputFile.getPath();

        if (outputFile.exists()) {
            if (!outputFile.isFile()) {
                throw new ValidationException(String.format("Output path exists but is not a file: %s", outputPath));
            }
            if (!outputFile.canWrite()) {
                throw new ValidationException(String.format("Cannot write to existing output file: %s", outputPath));
            }

            CLIErrorHandler.warn(String.format("Output file already exists and will be overwritten: %s", outputPath));
        } else {

            File parentDir = outputFile.getParentFile();
            if (parentDir == null) {
                parentDir = new File(".");
            }

            if (!parentDir.exists()) {
                throw new ValidationException(String.format("Output directory does not exist: %s", parentDir.getPath()));
            } else if (!parentDir.canWrite()) {
                throw new ValidationException(String.format("Cannot write to output directory: %s", parentDir.getPath()));
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
            CLIErrorHandler.warn(String.format("Format %s does not match extension %s", format, expectedExtension));
        }
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1 || lastDot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDot + 1);
    }

    private record ExecutionContext(
            PauseController pc,
            ChecksumCalculator calculator,
            ProgressReporter reporter,
            FSNode root,
            String path,
            Format format
    ) {}

    private void executeResume() {
        File resumeFile = new File(cmd.getOptionValue("resume"));
        System.out.printf("Resuming from: %s\n", resumeFile.getPath());

        Serializable memento = new StateManager().load(resumeFile);

        if (memento instanceof VerificationStateMemento vm) {
            ExecutionContext ctx = buildContext(vm.path(), vm.algorithm(), vm.format());
            runVerification(ctx, new File(vm.inputFilePath()), vm);
        } else if (memento instanceof CalculationStateMemento cm) {
            ExecutionContext ctx = buildContext(cm.path(), cm.algorithm(), cm.format());
            runCalculation(ctx, new File(cm.outputFilePath()), cm);
        } else {
            CLIErrorHandler.fatal("Invalid state file data.");
        }
    }

    private void executeVerification() {
        PauseController pc = new PauseController();
        String path = parsePath();
        File inputFile = parseChecksumsFile();
        Format format = parseFormat(inputFile.getName());
        FSNode root = new FSDirector(parseFollowLinks()).construct(path);
        report(root);

        ExecutionContext ctx = new ExecutionContext(
                pc, parseAlgorithm(pc), new ProgressReporter(root.getSizeInBytes()),
                root, path, format
        );

        runVerification(ctx, inputFile, null);
    }

    private void executeCalculation() {
        PauseController pc = new PauseController();
        String path = parsePath();
        File outputFile = parseOutput();
        Format format = parseFormat(outputFile.getName());
        FSNode root = new FSDirector(parseFollowLinks()).construct(path);

        if (root.getSizeInBytes() == 0) {
            CLIErrorHandler.warn(String.format("Provided path contains zero bytes: %s", path));
        }
        report(root);

        ExecutionContext ctx = new ExecutionContext(
                pc, parseAlgorithm(pc), new ProgressReporter(root.getSizeInBytes()),
                root, path, format
        );

        runCalculation(ctx, outputFile, null);
    }

    private void runVerification(ExecutionContext ctx, File inputFile, VerificationStateMemento memento) {
        ctx.pc().startPauseListener();

        ChecksumReader reader = switch (ctx.format()) {
            case JSON -> new JSONChecksumReader(inputFile.getPath());
            case TEXT -> new TextChecksumReader(inputFile.getPath());
        };

        HashComparator hc = new HashComparator(reader, ctx.calculator());
        hc.addObserver(ctx.reporter());

        if (memento != null) {
            hc.restoreFromMemento(memento.hashComparatorMemento());
            ctx.reporter().restoreFromMemento(memento.progressReporterMemento());
        }

        Map<String, Status> statuses = hc.compare(ctx.root());

        if (hc.isPaused()) {
            saveState(new VerificationStateMemento(
                    hc.saveToMemento(),
                    ctx.reporter().saveToMemento(),
                    ctx.path(),
                    inputFile.getPath(),
                    ctx.format(),
                    ctx.calculator().getAlgorithm()
            ));
        } else {
            try (ComparisonResultSerializer comparisonResultSerializer = new ComparisonResultSerializer(System.out)) {
                comparisonResultSerializer.write(statuses);
            }
        }
    }

    private void runCalculation(ExecutionContext ctx, File outputFile, CalculationStateMemento memento) {
        ctx.pc().startPauseListener();
        boolean append = (memento != null);

        try (FileOutputStream fos = new FileOutputStream(outputFile, append)) {
            ChecksumSerializer serializer = switch (ctx.format()) {
                case TEXT -> new TextChecksumSerializer(fos);
                case JSON -> new JSONChecksumSerializer(fos);
            };

            HashStreamWriter hsw = new HashStreamWriter(serializer, ctx.calculator());
            hsw.addObserver(ctx.reporter());

            if (memento != null) {
                hsw.restoreFromMemento(memento.hashStreamWriterMemento());
                ctx.reporter().restoreFromMemento(memento.progressReporterMemento());
            }

            hsw.write(ctx.root());

            if (hsw.isPaused()) {
                saveState(new CalculationStateMemento(
                        hsw.saveToMemento(),
                        ctx.reporter().saveToMemento(),
                        ctx.path(),
                        outputFile.getPath(),
                        ctx.format(),
                        ctx.calculator().getAlgorithm()
                ));
            } else {
                System.out.printf("\nDone! Results written to: %s\n", outputFile.getAbsolutePath());
            }
        } catch (IOException e) {
            CLIErrorHandler.fatal(e.getMessage());
        }
    }

    private ExecutionContext buildContext(String path, Algorithm algorithm, Format format) {
        PauseController pc = new PauseController();
        ChecksumCalculator calculator = switch (algorithm) {
            case MD5 -> new MD5Calculator(pc);
            case SHA1 -> new SHA1Calculator(pc);
            case SHA512 -> new SHA512Calculator(pc);
        };
        FSNode root = new FSDirector(parseFollowLinks()).construct(path);

        return new ExecutionContext(pc, calculator, new ProgressReporter(root.getSizeInBytes()), root, path, format);
    }

    private void saveState(Serializable memento) {
        File stateFile = parseStateFile();
        new StateManager().save(memento, stateFile);
        System.out.printf("\nProcess paused. State saved to: %s\n", stateFile.getAbsolutePath());
    }

    private ChecksumCalculator parseAlgorithm(PauseController pc) {
        if (cmd.hasOption("algorithm")) {
            return switch (cmd.getOptionValue("algorithm").toLowerCase()) {
                case "sha1" -> new SHA1Calculator(pc);
                case "sha512" -> new SHA512Calculator(pc);
                default -> new MD5Calculator(pc);
            };
        }
        return new MD5Calculator(pc);
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

    private static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    private void report(FSNode root) {
        ReportWriter rw = new ReportWriter(System.out);
        root.accept(rw);

        IO.println("\nTo pause the process press 'p' and then hit enter.");
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