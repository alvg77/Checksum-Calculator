package com.ChecksumCalculator.cli;

import com.ChecksumCalculator.calculator.ChecksumCalculator;
import com.ChecksumCalculator.calculator.MD5Calculator;
import com.ChecksumCalculator.calculator.SHA1Calculator;
import com.ChecksumCalculator.calculator.SHA512Calculator;
import com.ChecksumCalculator.exception.ValidationException;
import com.ChecksumCalculator.filesystem.builder.FSBuilder;
import com.ChecksumCalculator.filesystem.builder.FSDirector;
import com.ChecksumCalculator.filesystem.builder.FollowLinksFSBuilder;
import com.ChecksumCalculator.filesystem.builder.StandardFSBuilder;
import com.ChecksumCalculator.filesystem.node.FSNode;
import com.ChecksumCalculator.state.PauseController;
import org.apache.commons.cli.CommandLine;

import java.io.File;
import java.util.Set;

public class CLIConfig {
    private static final Set<String> VALID_ALGORITHMS = Set.of("md5", "sha1", "sha512");
    private static final Set<String> VALID_FORMATS = Set.of("text", "json");

    private final CommandLine cmd;
    private final Mode mode;

    public CLIConfig(CommandLine cmd) {
        this.cmd = cmd;
        this.mode = determineMode();
    }

    private Mode determineMode() {
        boolean hasChecksums = cmd.hasOption("checksums");
        boolean hasResume = cmd.hasOption("resume");

        if (hasChecksums && hasResume) {
            throw new ValidationException("Cannot have both --checksums and --resume at the same time");
        }

        if (hasChecksums) {
            return Mode.VERIFY;
        } else if (hasResume) {
            return Mode.RESUME;
        } else {
            return Mode.CALCULATE;
        }
    }

    public Mode getMode() {
        return mode;
    }

    public void validate() {
        validateAlgorithm();
        validateFormat();
        validatePath();

        switch (mode) {
            case VERIFY -> validateVerificationMode();
            case RESUME -> validateResumeMode();
            case CALCULATE -> validateCalculationMode();
        }
    }

    public void validateAlgorithm() {
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

    public void validateFormat() {
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

    public void validatePath() {
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

    public void validateVerificationMode() {
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

    public void validateResumeMode() {
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

    public void validateCalculationMode() {
        File outputFile = parseOutputFile();
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

    public void validateFormatExtensionConsistency(String fileName) {
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

    public String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1 || lastDot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDot + 1);
    }

    public ChecksumCalculator parseAlgorithm(PauseController pc) {
        if (cmd.hasOption("algorithm")) {
            return switch (cmd.getOptionValue("algorithm").toLowerCase()) {
                case "sha1" -> new SHA1Calculator(pc);
                case "sha512" -> new SHA512Calculator(pc);
                default -> new MD5Calculator(pc);
            };
        }
        return new MD5Calculator(pc);
    }

    public File parseOutputFile() {
        if (cmd.hasOption("output")) {
            return new File(cmd.getOptionValue("output"));
        }
        return new File("checksums.txt");
    }

    public Format parseFormat(String path) {
        if (cmd.hasOption("format")) {
            return switch (cmd.getOptionValue("format").toLowerCase()) {
                case "json" -> Format.JSON;
                default -> Format.TEXT;
            };
        }

        if (path.toLowerCase().endsWith(".json")) {
            return Format.JSON;
        }

        return Format.TEXT;
    }

    public FSBuilder parseFollowLinks() {
        if (cmd.hasOption("follow-links")) {
            return new FollowLinksFSBuilder();
        }
        return new StandardFSBuilder();
    }

    public File parseStateFile() {
        if (cmd.hasOption("state-file")) {
            return new File(cmd.getOptionValue("state-file"));
        }
        return new File("checksums.state");
    }

    public String parsePath() {
        if (cmd.hasOption("path")) {
            return cmd.getOptionValue("path");
        }
        return ".";
    }

    public File parseChecksumsFile() {
        if (cmd.hasOption("checksums")) {
            return new File(cmd.getOptionValue("checksums"));
        }
        return new File("checksums.txt");
    }

    public File parseResumeFile() {
        if (cmd.hasOption("resume-file")) {
            return new File(cmd.getOptionValue("resume-file"));
        }
        return new File("checksums.state");
    }

    public FSNode buildFS(String path) {
        FSBuilder builder = parseFollowLinks();
        return new FSDirector(builder).construct(path);

    }

    private static boolean isBlank(String string) {
        return string == null || string.trim().isEmpty();
    }
}
