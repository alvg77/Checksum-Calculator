package com.ChecksumCalculator.core.verification;

import com.ChecksumCalculator.cli.CLIErrorHandler;
import com.ChecksumCalculator.io.reader.ChecksumReader;
import com.ChecksumCalculator.core.calculator.ChecksumCalculator;
import com.ChecksumCalculator.filesystem.node.DirNode;
import com.ChecksumCalculator.filesystem.node.FSNode;
import com.ChecksumCalculator.filesystem.node.FileNode;
import com.ChecksumCalculator.filesystem.visitor.FSVisitor;
import com.ChecksumCalculator.progress.observer.Observable;
import com.ChecksumCalculator.progress.observer.Observer;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class HashComparator extends Observable implements FSVisitor, Observer {
    ChecksumCalculator calc;
    private final Map<String, String> hashes;
    private final Map<String, Status> statuses;

    public HashComparator(ChecksumReader reader, ChecksumCalculator calc) {
        this.calc = calc;
        statuses = new HashMap<>();
        hashes = reader.readHashes();
    }

    public Map<String, Status> compare(FSNode node) {
        node.accept(this);

        for (String path : hashes.keySet()) {
            if (!statuses.containsKey(path)) {
                statuses.put(path, Status.REMOVED);
            }
        }

        return statuses;
    }

    @Override
    public void visit(FileNode node) {
        String path = node.getPath();

        notifyObservers(this, node.getPath());
        try (FileInputStream fis = new FileInputStream(path)) {
            String checksum = calc.calculate(fis);

            if (hashes.containsKey(path)) {
                if (hashes.get(path).equals(checksum)) {
                    statuses.put(path, Status.OK);
                } else {
                    statuses.put(path, Status.MODIFIED);
                }
            } else {
                statuses.put(path, Status.NEW);
            }
        } catch (IOException e) {
            CLIErrorHandler.fatal(e.getMessage(), CLIErrorHandler.ErrorType.IO);
        }
    }

    @Override
    public void visit(DirNode node) {
        for (FSNode child : node.getChildren()) {
            child.accept(this);
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
}
