package com.ChecksumCalculator.cli;

import com.ChecksumCalculator.calculator.*;
import com.ChecksumCalculator.exception.ChecksumCalculatorException;
import com.ChecksumCalculator.exception.FileSystemException;
import com.ChecksumCalculator.filesystem.node.FSNode;
import com.ChecksumCalculator.filesystem.visitor.HashComparator;
import com.ChecksumCalculator.filesystem.visitor.HashStreamWriter;
import com.ChecksumCalculator.filesystem.visitor.ReportWriter;
import com.ChecksumCalculator.filesystem.visitor.Status;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

public class CommandExecutor {
    public void calculate(CLIConfig config) {
        PauseController pauseController = new PauseController();
        ChecksumCalculator checksumCalculator = config.parseAlgorithm(pauseController);
        String path = config.parsePath();
        FSNode root = config.buildFS(path);
        ProgressReporter reporter = new ProgressReporter(root.getSizeInBytes());

        if (root.getSizeInBytes() == 0) {
            CLIErrorHandler.warn(String.format("Provided path contains zero bytes: %s", config.parsePath()));
        }

        printReport(root);

        CommandContext ctx = new CommandContext(
                pauseController,
                checksumCalculator,
                new ProgressReporter(root.getSizeInBytes()),
                root,
                config.parseStateFile(),
                path,
                config.parseFormat(path),
                checksumCalculator.getAlgorithm()
        );

        executeCalculation(ctx, config.parseOutputFile(), null);
    }

    public void verify(CLIConfig config) {
        PauseController pauseController = new PauseController();
        ChecksumCalculator checksumCalculator = config.parseAlgorithm(pauseController);
        String path = config.parsePath();
        File inputFile = config.parseChecksumsFile();
        Format format = config.parseFormat(inputFile.getName());
        FSNode root = config.buildFS(path);

        printReport(root);

        CommandContext ctx = new CommandContext(
                pauseController,
                config.parseAlgorithm(pauseController),
                new ProgressReporter(root.getSizeInBytes()),
                root,
                config.parseStateFile(),
                path,
                format,
                checksumCalculator.getAlgorithm()
        );

        executeVerification(ctx, inputFile, null);
    }

    public void resume(CLIConfig config) {
        File resumeFile = config.parseResumeFile();
        System.out.println("Resuming file: " + resumeFile.getAbsolutePath());

        Serializable memento = new StateManager().load(resumeFile);

        if (memento instanceof CalculationStateMemento calculationStateMemento) {
            resumeCalculation(calculationStateMemento, config);
        } else if (memento instanceof VerificationStateMemento verificationStateMemento) {
            resumeVerification(verificationStateMemento, config);
        } else {
            throw new ChecksumCalculatorException("Invalid state file contents!");
        }
    }

    private record CommandContext(PauseController pauseController, ChecksumCalculator checksumCalculator,
                                  ProgressReporter progressReporter, FSNode root,
                                  File stateFile, String path, Format format,
                                  Algorithm algorithm) {
    }

    private void executeCalculation(CommandContext ctx, File outputFile, CalculationStateMemento memento) {
        ctx.pauseController.startPauseListener();
        boolean append = (memento != null);

        try (FileOutputStream fos = new FileOutputStream(outputFile, append)) {
            ChecksumSerializer checksumSerializer = switch (ctx.format()) {
                case TEXT -> new TextChecksumSerializer(fos);
                case JSON -> new JSONChecksumSerializer(fos);
            };

            HashStreamWriter hashStreamWriter = new HashStreamWriter(checksumSerializer, ctx.checksumCalculator());
            hashStreamWriter.addObserver(ctx.progressReporter());

            if (memento != null) {
                hashStreamWriter.restoreFromMemento(memento.hashStreamWriterMemento());
                ctx.progressReporter().restoreFromMemento(memento.progressReporterMemento());
            }

            hashStreamWriter.write(ctx.root());

            if (ctx.pauseController().isPauseRequested()) {
                StateManager stateManager = new StateManager();
                stateManager.save(new CalculationStateMemento(
                        hashStreamWriter.saveToMemento(),
                        ctx.progressReporter().saveToMemento(),
                        ctx.path(),
                        outputFile.getPath(),
                        ctx.format(),
                        ctx.checksumCalculator().getAlgorithm()
                ), ctx.stateFile());
            } else {
                System.out.printf("\nDone! Results written to: %s\n", outputFile.getAbsolutePath());
            }
        } catch (IOException e) {
            throw new FileSystemException(String.format("Error while writing checksums to file: %s", e.getMessage()), e);
        }
    }

    private void executeVerification(CommandContext ctx, File inputFile, VerificationStateMemento memento) {
        ctx.pauseController().startPauseListener();

        ChecksumReader reader = switch (ctx.format()) {
            case JSON -> new JSONChecksumReader(inputFile.getPath());
            case TEXT -> new TextChecksumReader(inputFile.getPath());
        };

        HashComparator hashComparator = new HashComparator(reader, ctx.checksumCalculator());
        hashComparator.addObserver(ctx.progressReporter());

        if (memento != null) {
            hashComparator.restoreFromMemento(memento.hashComparatorMemento());
            ctx.progressReporter().restoreFromMemento(memento.progressReporterMemento());
        }

        Map<String, Status> statuses = hashComparator.compare(ctx.root());

        if (ctx.pauseController().isPauseRequested()) {
            StateManager stateManager = new StateManager();
            stateManager.save(new VerificationStateMemento(
                    hashComparator.saveToMemento(),
                    ctx.progressReporter().saveToMemento(),
                    ctx.path(),
                    inputFile.getPath(),
                    ctx.format(),
                    ctx.checksumCalculator().getAlgorithm()
            ), ctx.stateFile());
        } else {
            try (ComparisonResultSerializer comparisonResultSerializer = new ComparisonResultSerializer(System.out)) {
                comparisonResultSerializer.write(statuses);
            }
        }
    }

    private void resumeCalculation(CalculationStateMemento memento, CLIConfig config) {
        PauseController pauseController = new PauseController();
        ChecksumCalculator checksumCalculator = switch (memento.algorithm()) {
            case MD5 -> new MD5Calculator(pauseController);
            case SHA1 -> new SHA1Calculator(pauseController);
            case SHA512 -> new SHA512Calculator(pauseController);
        };
        String path = memento.path();
        FSNode root = config.buildFS(path);

        CommandContext ctx = new CommandContext(
                pauseController,
                checksumCalculator,
                new ProgressReporter(root.getSizeInBytes()),
                root,
                config.parseStateFile(),
                path,
                memento.format(),
                memento.algorithm()
        );

        executeCalculation(ctx, new File(memento.outputFilePath()), memento);
    }

    private void resumeVerification(VerificationStateMemento memento, CLIConfig config) {
        PauseController pauseController = new PauseController();
        ChecksumCalculator checksumCalculator = switch (memento.algorithm()) {
            case MD5 -> new MD5Calculator(pauseController);
            case SHA1 -> new SHA1Calculator(pauseController);
            case SHA512 -> new SHA512Calculator(pauseController);
        };
        String path = memento.path();
        FSNode root = config.buildFS(path);

        CommandContext ctx = new CommandContext(
                pauseController,
                checksumCalculator,
                new ProgressReporter(root.getSizeInBytes()),
                root,
                config.parseStateFile(),
                path,
                memento.format(),
                memento.algorithm()
        );

        executeVerification(ctx, new File(memento.inputFilePath()), memento);
    }

    private ChecksumSerializer createChecksumSerializer(Format format, FileOutputStream fos) {
        return switch (format) {
            case TEXT -> new TextChecksumSerializer(fos);
            case JSON -> new JSONChecksumSerializer(fos);
        };
    }

    private ChecksumReader createChecksumReader(Format format, File inputFile) {
        return switch (format) {
            case JSON -> new JSONChecksumReader(inputFile.getPath());
            case TEXT -> new TextChecksumReader(inputFile.getPath());
        };
    }

    private void printReport(FSNode root) {
        ReportWriter reportWriter = new ReportWriter(System.out);
        root.accept(reportWriter);

        IO.println("\nTo pause the process press 'p' and then hit enter");
        IO.println("\nPress any key to continue...");
        IO.readln();
    }
}
