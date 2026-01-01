package com.ChecksumCalculator.filesystem.visitor;

import com.ChecksumCalculator.cli.CLIErrorHandler;
import com.ChecksumCalculator.core.calculator.ChecksumCalculator;
import com.ChecksumCalculator.filesystem.node.DirNode;
import com.ChecksumCalculator.filesystem.node.FSNode;
import com.ChecksumCalculator.filesystem.node.FileNode;
import com.ChecksumCalculator.io.ChecksumData;
import com.ChecksumCalculator.io.serializer.checksum.ChecksumSerializer;
import com.ChecksumCalculator.progress.observer.Observable;
import com.ChecksumCalculator.progress.observer.Observer;
import com.ChecksumCalculator.state.memento.HashStreamWriterMemento;
import com.ChecksumCalculator.state.PauseMessage;

import java.io.*;
import java.util.HashSet;
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
//        if (!paused) {
            checksumSerializer.close();

    }

    @Override
    public void visit(FileNode node) {
        String nodePath = node.getPath();
        if (processedFiles.contains(nodePath)) {
            CLIErrorHandler.warn("File " + nodePath + " has already been processed, skipping...");
            return;
        }

        File f = new File(nodePath);

        notifyObservers(this, nodePath);
        try {
            String checksum = calc.calculate(new FileInputStream(f));

            if (paused) {
//                checksumSerializer.close();
                return;
            }

            assert checksum != null;

            checksumSerializer.write(new ChecksumData(checksum, f.getPath(), f.length()));
            processedFiles.add(nodePath);
        } catch (IOException e) {
            CLIErrorHandler.fatal(e.getMessage(), CLIErrorHandler.ErrorType.IO);
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

    public boolean isPaused() {
        return paused;
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
