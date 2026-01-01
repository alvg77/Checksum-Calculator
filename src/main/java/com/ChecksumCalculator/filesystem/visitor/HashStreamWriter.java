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
import com.ChecksumCalculator.state.HashStreamWriterMemento;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

public class HashStreamWriter extends Observable implements FSVisitor, Observer {
    ChecksumSerializer checksumSerializer;
    ChecksumCalculator calc;

    Set<String> processedFiles;

    public HashStreamWriter(ChecksumSerializer serializer, ChecksumCalculator calc) {
        checksumSerializer = serializer;
        this.calc = calc;
        processedFiles = new HashSet<>();
    }

    public void write(FSNode root) {
       root.accept(this);
       checksumSerializer.close();
    }

    @Override
    public void visit(FileNode node) {
        String nodePath = node.getPath();
        if (processedFiles.contains(nodePath)) {
            CLIErrorHandler.warn("File " + nodePath + " has already been processed, skipping...");
            return;
        }

        processedFiles.add(nodePath);

        File f = new File(nodePath);

        notifyObservers(this, nodePath);
        try {
            String checksum = calc.calculate(new FileInputStream(f));
            checksumSerializer.write(new ChecksumData(checksum, f.getPath(), f.length()));
        } catch (IOException e) {
            CLIErrorHandler.fatal(e.getMessage(), CLIErrorHandler.ErrorType.IO);
        }
    }

    @Override
    public void visit(DirNode node) {
        for (FSNode fn : node.getChildren()) {
            String fnPath = fn.getPath();
            if (processedFiles.contains(fnPath)) {
                CLIErrorHandler.warn("File " + fnPath + " has already been processed, skipping...");
                continue;
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
        notifyObservers(source, msg);
    }

    // TODO:
    public HashStreamWriterMemento createMemento() {
        return null;
    }

    public void loadFromMemento(HashStreamWriterMemento memento) {

    }
}
