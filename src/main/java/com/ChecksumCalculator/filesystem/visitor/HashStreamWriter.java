package com.ChecksumCalculator.filesystem.visitor;

import com.ChecksumCalculator.calculator.ChecksumCalculator;
import com.ChecksumCalculator.cli.CLIErrorHandler;
import com.ChecksumCalculator.filesystem.node.DirNode;
import com.ChecksumCalculator.filesystem.node.FSNode;
import com.ChecksumCalculator.filesystem.node.FileNode;
import com.ChecksumCalculator.io.ChecksumData;
import com.ChecksumCalculator.io.serializer.checksum.ChecksumSerializer;
import com.ChecksumCalculator.progress.observer.Observable;
import com.ChecksumCalculator.progress.observer.Observer;
import com.ChecksumCalculator.state.PauseMessage;
import com.ChecksumCalculator.state.memento.HashStreamWriterMemento;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class HashStreamWriter extends Observable implements FSVisitor, Observer {
    private final ChecksumSerializer checksumSerializer;
    private final ChecksumCalculator calc;

    private Set<String> processedFiles;
    private boolean paused;

    public HashStreamWriter(ChecksumSerializer serializer, ChecksumCalculator calc) {
        this.checksumSerializer = serializer;
        this.calc = calc;
        calc.addObserver(this);
        this.processedFiles = new HashSet<>();
        this.paused = false;
    }

    public void write(FSNode root) {
        root.accept(this);
        checksumSerializer.close();
    }

    @Override
    public void visit(FileNode node) {
        String path = node.getPath();

        if (processedFiles.contains(path)) {
            return;
        }

        File f = new File(path);

        if (!f.exists()) {
            CLIErrorHandler.warn(String.format("Skipping %s: %s", node, "File no longer exists"));
            return;
        }

        if (!f.canRead()) {
            CLIErrorHandler.warn(String.format("Skipping %s: %s", node, "File is not readable"));
            return;
        }

        notifyObservers(this, path);

        try {
            Optional<String> checksum = calc.calculate(new FileInputStream(f));

            if (paused) {
                return;
            }

            if (checksum.isEmpty()) {
                CLIErrorHandler.warn(String.format("File %s has no calculated checksum.", path));
                return;
            }

            checksumSerializer.write(new ChecksumData(checksum.get(), f.getPath(), f.length()));
            processedFiles.add(path);
        } catch (IOException e) {
            CLIErrorHandler.warn(String.format("%s occurred while processing %s. Skipping...", path, e.getMessage()));
        }
    }

    @Override
    public void visit(DirNode node) {
        for (FSNode fn : node.getChildren()) {
            if (paused) {
                return;
            }
            fn.accept(this);
        }
    }

    @Override
    public void addObserver(Observer observer) {
        super.addObserver(observer);
        calc.addObserver(observer);
    }

    @Override
    public void update(Observable source, Object msg) {
        if (source instanceof ChecksumCalculator && msg instanceof PauseMessage) {
            paused = true;
        }
    }

    public HashStreamWriterMemento saveToMemento() {
        return new HashStreamWriterMemento(
                new HashSet<>(processedFiles)
        );
    }

    public void restoreFromMemento(HashStreamWriterMemento memento) {
        this.processedFiles = new HashSet<>(memento.processedFiles());
    }
}
