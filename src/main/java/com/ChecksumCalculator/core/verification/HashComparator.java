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
import com.ChecksumCalculator.state.PauseMessage;
import com.ChecksumCalculator.state.memento.HashComparatorMemento;
import com.ChecksumCalculator.state.memento.ProgressReporterMemento;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class HashComparator extends Observable implements FSVisitor, Observer {
    ChecksumCalculator calc;
    private final Map<String, String> hashes;
    private final Map<String, Status> statuses;

    private boolean paused;

    public HashComparator(ChecksumReader reader, ChecksumCalculator calc) {
        this.calc = calc;
        calc.addObserver(this);
        this.statuses = new HashMap<>();
        this.hashes = reader.readHashes();
        this.paused = false;
    }

    public Map<String, Status> compare(FSNode node) {
        node.accept(this);

        if (!paused) {
            for (String path : hashes.keySet()) {
                if (!statuses.containsKey(path)) {
                    statuses.put(path, Status.REMOVED);
                }
            }

            return statuses;
        }

        return null;
    }

    @Override
    public void visit(FileNode node) {
        String path = node.getPath();

        if (statuses.containsKey(path)) {
            return;
        }

        notifyObservers(this, node.getPath());
        try (FileInputStream fis = new FileInputStream(path)) {
            String checksum = calc.calculate(fis);

            if (paused) {
                return;
            }

            assert checksum != null;

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
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void visit(DirNode node) {
        for (FSNode child : node.getChildren()) {
            if (paused) {
                return;
            }
            child.accept(this);
        }
    }

    public boolean isPaused() {
        return paused;
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

    public HashComparatorMemento saveToMemento() { return new HashComparatorMemento(new HashMap<>(statuses)); }

    public void restoreFromMemento(HashComparatorMemento memento) {
        Map<String, Status> tempStatuses = memento.statuses();
        for (String status : memento.statuses().keySet()) {
            statuses.put(status, tempStatuses.get(status));
        }
    }
}
