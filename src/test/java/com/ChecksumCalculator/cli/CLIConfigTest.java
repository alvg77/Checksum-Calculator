package com.ChecksumCalculator.cli;

import com.ChecksumCalculator.calculator.ChecksumCalculator;
import com.ChecksumCalculator.calculator.MD5Calculator;
import com.ChecksumCalculator.calculator.SHA1Calculator;
import com.ChecksumCalculator.calculator.SHA512Calculator;
import com.ChecksumCalculator.exception.ValidationException;
import com.ChecksumCalculator.filesystem.builder.FollowLinksFSBuilder;
import com.ChecksumCalculator.filesystem.builder.StandardFSBuilder;
import com.ChecksumCalculator.filesystem.node.DirNode;
import com.ChecksumCalculator.filesystem.node.FSNode;
import com.ChecksumCalculator.filesystem.node.FileNode;
import com.ChecksumCalculator.state.PauseController;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class CLIConfigTest {

    @TempDir
    Path tempDir;

    private Options options;
    private CommandLineParser parser;

    @BeforeEach
    void setUp() {
        options = buildOptions();
        parser = new DefaultParser();
    }

    private Options buildOptions() {
        Options opts = new Options();
        opts.addOption("h", "help", false, "print this message");
        opts.addOption("p", "path", true, "target path");
        opts.addOption("L", "follow-links", false, "follow symbolic links");
        opts.addOption("a", "algorithm", true, "algorithm");
        opts.addOption("o", "output", true, "output file");
        opts.addOption("f", "format", true, "output format");
        opts.addOption("c", "checksums", true, "checksums file");
        opts.addOption("s", "state-file", true, "state file");
        opts.addOption("r", "resume", true, "resume file");
        return opts;
    }

    private CLIConfig createConfig(String... args) throws ParseException {
        CommandLine cmd = parser.parse(options, args);
        return new CLIConfig(cmd);
    }

    @Test
    void getMode_returnsCalculate_byDefault() throws ParseException {
        CLIConfig config = createConfig();

        assertEquals(Mode.CALCULATE, config.getMode());
    }

    @Test
    void getMode_returnsVerify_whenChecksumsOptionPresent() throws ParseException {
        Path checksumFile = tempDir.resolve("checksums.txt");
        CLIConfig config = createConfig("-c", checksumFile.toString());

        assertEquals(Mode.VERIFY, config.getMode());
    }

    @Test
    void getMode_returnsResume_whenResumeOptionPresent() throws ParseException {
        Path stateFile = tempDir.resolve("state.dat");
        CLIConfig config = createConfig("-r", stateFile.toString());

        assertEquals(Mode.RESUME, config.getMode());
    }

    @Test
    void constructor_throwsValidationException_whenBothChecksumsAndResumePresent() {
        assertThrows(ValidationException.class, () -> createConfig("-c", "checksums.txt", "-r", "state.dat"));
    }

    @Test
    void validateAlgorithm_passesForMd5() throws ParseException {
        CLIConfig config = createConfig("-a", "md5");

        assertDoesNotThrow(config::validateAlgorithm);
    }

    @Test
    void validateAlgorithm_passesForSha1() throws ParseException {
        CLIConfig config = createConfig("-a", "sha1");

        assertDoesNotThrow(config::validateAlgorithm);
    }

    @Test
    void validateAlgorithm_passesForSha512() throws ParseException {
        CLIConfig config = createConfig("-a", "sha512");

        assertDoesNotThrow(config::validateAlgorithm);
    }

    @Test
    void validateAlgorithm_isCaseInsensitive() throws ParseException {
        CLIConfig config = createConfig("-a", "MD5");

        assertDoesNotThrow(config::validateAlgorithm);
    }

    @Test
    void validateAlgorithm_throwsForInvalidAlgorithm() throws ParseException {
        CLIConfig config = createConfig("-a", "invalid");

        ValidationException exception = assertThrows(ValidationException.class, config::validateAlgorithm);
        assertTrue(exception.getMessage().contains("Unknown algorithm"));
        assertTrue(exception.getMessage().contains("invalid"));
    }

    @Test
    void validateAlgorithm_throwsForEmptyAlgorithm() throws ParseException {
        CLIConfig config = createConfig("-a", "");

        ValidationException exception = assertThrows(ValidationException.class, config::validateAlgorithm);
        assertTrue(exception.getMessage().contains("cannot be empty"));
    }

    @Test
    void validateAlgorithm_passesWhenNoAlgorithmSpecified() throws ParseException {
        CLIConfig config = createConfig();

        assertDoesNotThrow(config::validateAlgorithm);
    }

    @Test
    void validateFormat_passesForText() throws ParseException {
        CLIConfig config = createConfig("-f", "text");

        assertDoesNotThrow(config::validateFormat);
    }

    @Test
    void validateFormat_passesForJson() throws ParseException {
        CLIConfig config = createConfig("-f", "json");

        assertDoesNotThrow(config::validateFormat);
    }

    @Test
    void validateFormat_isCaseInsensitive() throws ParseException {
        CLIConfig config = createConfig("-f", "JSON");

        assertDoesNotThrow(config::validateFormat);
    }

    @Test
    void validateFormat_throwsForInvalidFormat() throws ParseException {
        CLIConfig config = createConfig("-f", "xml");

        ValidationException exception = assertThrows(ValidationException.class, config::validateFormat);
        assertTrue(exception.getMessage().contains("Unknown format"));
    }

    @Test
    void validateFormat_throwsForEmptyFormat() throws ParseException {
        CLIConfig config = createConfig("-f", "");

        ValidationException exception = assertThrows(ValidationException.class, config::validateFormat);
        assertTrue(exception.getMessage().contains("cannot be empty"));
    }

    @Test
    void validateFormat_passesWhenNoFormatSpecified() throws ParseException {
        CLIConfig config = createConfig();

        assertDoesNotThrow(config::validateFormat);
    }

    @Test
    void validatePath_passesForExistingDirectory() throws ParseException {
        CLIConfig config = createConfig("-p", tempDir.toString());

        assertDoesNotThrow(config::validatePath);
    }

    @Test
    void validatePath_passesForExistingFile() throws ParseException, IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "content");

        CLIConfig config = createConfig("-p", file.toString());

        assertDoesNotThrow(config::validatePath);
    }

    @Test
    void validatePath_throwsForNonExistentPath() throws ParseException {
        CLIConfig config = createConfig("-p", "/nonexistent/path");

        ValidationException exception = assertThrows(ValidationException.class, config::validatePath);
        assertTrue(exception.getMessage().contains("does not exist"));
    }

    @Test
    void validatePath_throwsForEmptyPath() throws ParseException {
        CLIConfig config = createConfig("-p", "");

        ValidationException exception = assertThrows(ValidationException.class, config::validatePath);
        assertTrue(exception.getMessage().contains("cannot be empty"));
    }

    @Test
    void validatePath_usesCurrentDirectoryByDefault() throws ParseException {
        CLIConfig config = createConfig();

        assertDoesNotThrow(config::validatePath);
    }

    @Test
    void validateVerificationMode_passesForExistingChecksumsFile() throws ParseException, IOException {
        Path checksumFile = tempDir.resolve("checksums.txt");
        Files.writeString(checksumFile, "abc123 /file.txt 100");

        CLIConfig config = createConfig("-c", checksumFile.toString());

        assertDoesNotThrow(config::validateVerificationMode);
    }

    @Test
    void validateVerificationMode_throwsForNonExistentChecksumsFile() throws ParseException {
        CLIConfig config = createConfig("-c", "/nonexistent/checksums.txt");

        ValidationException exception = assertThrows(ValidationException.class, config::validateVerificationMode);
        assertTrue(exception.getMessage().contains("does not exist"));
    }

    @Test
    void validateVerificationMode_throwsForDirectory() throws ParseException {
        CLIConfig config = createConfig("-c", tempDir.toString());

        ValidationException exception = assertThrows(ValidationException.class, config::validateVerificationMode);
        assertTrue(exception.getMessage().contains("not a file"));
    }

    @Test
    void validateVerificationMode_throwsForEmptyPath() throws ParseException {
        CLIConfig config = createConfig("-c", "");

        ValidationException exception = assertThrows(ValidationException.class, config::validateVerificationMode);
        assertTrue(exception.getMessage().contains("cannot be empty"));
    }

    @Test
    void validateResumeMode_passesForExistingStateFile() throws ParseException, IOException {
        Path stateFile = tempDir.resolve("state.dat");
        Files.writeString(stateFile, "state data");

        CLIConfig config = createConfig("-r", stateFile.toString());

        assertDoesNotThrow(config::validateResumeMode);
    }

    @Test
    void validateResumeMode_throwsForNonExistentStateFile() throws ParseException {
        CLIConfig config = createConfig("-r", "/nonexistent/state.dat");

        ValidationException exception = assertThrows(ValidationException.class, config::validateResumeMode);
        assertTrue(exception.getMessage().contains("does not exist"));
    }

    @Test
    void validateResumeMode_throwsForDirectory() throws ParseException {
        CLIConfig config = createConfig("-r", tempDir.toString());

        ValidationException exception = assertThrows(ValidationException.class, config::validateResumeMode);
        assertTrue(exception.getMessage().contains("not a file"));
    }

    @Test
    void validateResumeMode_throwsForEmptyPath() throws ParseException {
        CLIConfig config = createConfig("-r", "");

        ValidationException exception = assertThrows(ValidationException.class, config::validateResumeMode);
        assertTrue(exception.getMessage().contains("cannot be empty"));
    }

    @Test
    void validateCalculationMode_passesForNewOutputFile() throws ParseException {
        Path outputFile = tempDir.resolve("output.txt");

        CLIConfig config = createConfig("-o", outputFile.toString());

        assertDoesNotThrow(config::validateCalculationMode);
    }

    @Test
    void validateCalculationMode_passesForExistingWritableFile() throws ParseException, IOException {
        Path outputFile = tempDir.resolve("output.txt");
        Files.writeString(outputFile, "existing content");

        CLIConfig config = createConfig("-o", outputFile.toString());

        assertDoesNotThrow(config::validateCalculationMode);
    }

    @Test
    void validateCalculationMode_throwsWhenOutputIsDirectory() throws ParseException {
        CLIConfig config = createConfig("-o", tempDir.toString());

        ValidationException exception = assertThrows(ValidationException.class, config::validateCalculationMode);
        assertTrue(exception.getMessage().contains("not a file"));
    }

    @Test
    void validateCalculationMode_throwsWhenOutputDirectoryDoesNotExist() throws ParseException {
        CLIConfig config = createConfig("-o", "/nonexistent/dir/output.txt");

        ValidationException exception = assertThrows(ValidationException.class, config::validateCalculationMode);
        assertTrue(exception.getMessage().contains("does not exist"));
    }

    @Test
    void validateFormatExtensionConsistency_warnsWhenJsonFormatWithTxtExtension() throws ParseException {
        CLIConfig config = createConfig("-f", "json");

        assertDoesNotThrow(() -> config.validateFormatExtensionConsistency("output.txt"));
    }

    @Test
    void validateFormatExtensionConsistency_warnsWhenTextFormatWithJsonExtension() throws ParseException {
        CLIConfig config = createConfig("-f", "text");

        assertDoesNotThrow(() -> config.validateFormatExtensionConsistency("output.json"));
    }

    @Test
    void validateFormatExtensionConsistency_passesWhenFormatMatchesExtension() throws ParseException {
        CLIConfig config = createConfig("-f", "json");

        assertDoesNotThrow(() -> config.validateFormatExtensionConsistency("output.json"));
    }

    @Test
    void validateFormatExtensionConsistency_skipsWhenNoFormatSpecified() throws ParseException {
        CLIConfig config = createConfig();

        assertDoesNotThrow(() -> config.validateFormatExtensionConsistency("output.txt"));
    }

    @Test
    void getFileExtension_returnsExtension() throws ParseException {
        CLIConfig config = createConfig();

        assertEquals("txt", config.getFileExtension("file.txt"));
    }

    @Test
    void getFileExtension_returnsLastExtension() throws ParseException {
        CLIConfig config = createConfig();

        assertEquals("gz", config.getFileExtension("file.tar.gz"));
    }

    @Test
    void getFileExtension_returnsEmptyForNoExtension() throws ParseException {
        CLIConfig config = createConfig();

        assertEquals("", config.getFileExtension("file"));
    }

    @Test
    void getFileExtension_returnsEmptyForDotAtEnd() throws ParseException {
        CLIConfig config = createConfig();

        assertEquals("", config.getFileExtension("file."));
    }

    @Test
    void parseAlgorithm_returnsMD5Calculator_byDefault() throws ParseException {
        CLIConfig config = createConfig();
        PauseController pc = new PauseController();

        ChecksumCalculator calculator = config.parseAlgorithm(pc);

        assertInstanceOf(MD5Calculator.class, calculator);
    }

    @Test
    void parseAlgorithm_returnsMD5Calculator_forMd5() throws ParseException {
        CLIConfig config = createConfig("-a", "md5");
        PauseController pc = new PauseController();

        ChecksumCalculator calculator = config.parseAlgorithm(pc);

        assertInstanceOf(MD5Calculator.class, calculator);
    }

    @Test
    void parseAlgorithm_returnsSHA1Calculator_forSha1() throws ParseException {
        CLIConfig config = createConfig("-a", "sha1");
        PauseController pc = new PauseController();

        ChecksumCalculator calculator = config.parseAlgorithm(pc);

        assertInstanceOf(SHA1Calculator.class, calculator);
    }

    @Test
    void parseAlgorithm_returnsSHA512Calculator_forSha512() throws ParseException {
        CLIConfig config = createConfig("-a", "sha512");
        PauseController pc = new PauseController();

        ChecksumCalculator calculator = config.parseAlgorithm(pc);

        assertInstanceOf(SHA512Calculator.class, calculator);
    }

    @Test
    void parseAlgorithm_isCaseInsensitive() throws ParseException {
        CLIConfig config = createConfig("-a", "SHA1");
        PauseController pc = new PauseController();

        ChecksumCalculator calculator = config.parseAlgorithm(pc);

        assertInstanceOf(SHA1Calculator.class, calculator);
    }

    @Test
    void parseOutputFile_returnsSpecifiedFile() throws ParseException {
        CLIConfig config = createConfig("-o", "/path/to/output.txt");

        File outputFile = config.parseOutputFile();

        if (OS.current() == OS.WINDOWS) {
            assertEquals("\\path\\to\\output.txt", outputFile.getPath());
        } else {
            assertEquals("/path/to/output.txt", outputFile.getPath());
        }
    }

    @Test
    void parseOutputFile_returnsDefault_whenNotSpecified() throws ParseException {
        CLIConfig config = createConfig();

        File outputFile = config.parseOutputFile();

        assertEquals("checksums.txt", outputFile.getPath());
    }

    @Test
    void parseFormat_returnsText_byDefault() throws ParseException {
        CLIConfig config = createConfig();

        assertEquals(Format.TEXT, config.parseFormat("file.txt"));
    }

    @Test
    void parseFormat_returnsJson_whenSpecified() throws ParseException {
        CLIConfig config = createConfig("-f", "json");

        assertEquals(Format.JSON, config.parseFormat("file.txt"));
    }

    @Test
    void parseFormat_returnsText_whenSpecified() throws ParseException {
        CLIConfig config = createConfig("-f", "text");

        assertEquals(Format.TEXT, config.parseFormat("file.json"));
    }

    @Test
    void parseFormat_infersJsonFromExtension() throws ParseException {
        CLIConfig config = createConfig();

        assertEquals(Format.JSON, config.parseFormat("file.json"));
    }

    @Test
    void parseFormat_infersJsonFromExtension_caseInsensitive() throws ParseException {
        CLIConfig config = createConfig();

        assertEquals(Format.JSON, config.parseFormat("file.JSON"));
    }

    @Test
    void parseFollowLinks_returnsStandardBuilder_byDefault() throws ParseException {
        CLIConfig config = createConfig();

        assertInstanceOf(StandardFSBuilder.class, config.parseFollowLinks());
    }

    @Test
    void parseFollowLinks_returnsFollowLinksBuilder_whenFlagSet() throws ParseException {
        CLIConfig config = createConfig("-L");

        assertInstanceOf(FollowLinksFSBuilder.class, config.parseFollowLinks());
    }

    @Test
    void parseStateFile_returnsSpecifiedFile() throws ParseException {
        CLIConfig config = createConfig("-s", "/path/to/state.dat");

        File stateFile = config.parseStateFile();

        if (OS.current() == OS.WINDOWS) {
            assertEquals("\\path\\to\\state.dat", stateFile.getPath());
        } else {
            assertEquals("/path/to/state.dat", stateFile.getPath());
        }
    }

    @Test
    void parseStateFile_returnsDefault_whenNotSpecified() throws ParseException {
        CLIConfig config = createConfig();

        File stateFile = config.parseStateFile();

        assertEquals("checksums.state", stateFile.getPath());
    }

    @Test
    void parsePath_returnsSpecifiedPath() throws ParseException {
        CLIConfig config = createConfig("-p", "/path/to/dir");

        assertEquals("/path/to/dir", config.parsePath());
    }

    @Test
    void parsePath_returnsCurrentDirectory_byDefault() throws ParseException {
        CLIConfig config = createConfig();

        assertEquals(".", config.parsePath());
    }

    @Test
    void parseChecksumsFile_returnsSpecifiedFile() throws ParseException {
        CLIConfig config = createConfig("-c", "/path/to/checksums.txt");

        File checksumsFile = config.parseChecksumsFile();

        if (OS.current() == OS.WINDOWS) {
            assertEquals("\\path\\to\\checksums.txt", checksumsFile.getPath());
        } else {
            assertEquals("/path/to/checksums.txt", checksumsFile.getPath());
        }
    }

    @Test
    void parseChecksumsFile_returnsDefault_whenNotSpecified() throws ParseException {
        CLIConfig config = createConfig();

        File checksumsFile = config.parseChecksumsFile();

        assertEquals("checksums.txt", checksumsFile.getPath());
    }

    @Test
    void parseResumeFile_returnsDefault_whenNotSpecified() throws ParseException {
        CLIConfig config = createConfig();

        File resumeFile = config.parseResumeFile();

        assertEquals("checksums.state", resumeFile.getPath());
    }

    @Test
    void buildFS_buildsFileNode_forFile() throws ParseException, IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "content");

        CLIConfig config = createConfig();
        FSNode node = config.buildFS(file.toString());

        assertInstanceOf(FileNode.class, node);
        assertEquals(file.toString(), node.getPath());
    }

    @Test
    void buildFS_buildsDirNode_forDirectory() throws ParseException, IOException {
        Path dir = tempDir.resolve("subdir");
        Files.createDirectory(dir);

        CLIConfig config = createConfig();
        FSNode node = config.buildFS(dir.toString());

        assertInstanceOf(DirNode.class, node);
        assertEquals(dir.toString(), node.getPath());
    }

    @Test
    void buildFS_includesChildren_forDirectory() throws ParseException, IOException {
        Path dir = tempDir.resolve("subdir");
        Files.createDirectory(dir);
        Files.writeString(dir.resolve("file1.txt"), "content1");
        Files.writeString(dir.resolve("file2.txt"), "content2");

        CLIConfig config = createConfig();
        FSNode node = config.buildFS(dir.toString());

        DirNode dirNode = (DirNode) node;
        assertEquals(2, dirNode.getChildren().size());
    }

    @Test
    void validate_passesForValidCalculationMode() throws ParseException {
        Path outputFile = tempDir.resolve("output.txt");

        CLIConfig config = createConfig("-p", tempDir.toString(), "-o", outputFile.toString(), "-a", "md5", "-f", "text");

        assertDoesNotThrow(config::validate);
    }

    @Test
    void validate_passesForValidVerificationMode() throws ParseException, IOException {
        Path checksumFile = tempDir.resolve("checksums.txt");
        Files.writeString(checksumFile, "abc123 /file.txt 100");

        CLIConfig config = createConfig("-p", tempDir.toString(), "-c", checksumFile.toString());

        assertDoesNotThrow(config::validate);
    }

    @Test
    void validate_passesForValidResumeMode() throws ParseException, IOException {
        Path stateFile = tempDir.resolve("state.dat");
        Files.writeString(stateFile, "state data");

        CLIConfig config = createConfig("-r", stateFile.toString());

        assertDoesNotThrow(config::validate);
    }
}