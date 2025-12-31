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

import java.io.*;

public class HashStreamWriter extends Observable implements FSVisitor, Observer {
    ChecksumSerializer checksumSerializer;
    ChecksumCalculator calc;

    public HashStreamWriter(ChecksumSerializer serializer, ChecksumCalculator calc) {
        checksumSerializer = serializer;
        this.calc = calc;
    }

    public void write(FSNode root) {
       root.accept(this);
       checksumSerializer.close();
    }

    @Override
    public void visit(FileNode node) {
        File f = new File(node.getPath());

        notifyObservers(this, node.getPath());
        try {
            String checksum = calc.calculate(new FileInputStream(f));
            checksumSerializer.write(new ChecksumData(checksum, f.getPath(), f.length()));
        } catch (IOException e) {
            CLIErrorHandler.fatal(e.getMessage());
        }
    }

    @Override
    public void visit(DirNode node) {
        for (FSNode fn : node.getChildren()) {
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
}
