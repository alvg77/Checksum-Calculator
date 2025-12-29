package com.ChecksumCalculator.filesystem.visitor;

import com.ChecksumCalculator.calculator.ChecksumCalculator;
import com.ChecksumCalculator.filesystem.node.DirNode;
import com.ChecksumCalculator.filesystem.node.FSNode;
import com.ChecksumCalculator.filesystem.node.FileNode;

import java.io.*;

public class HashStreamWriter implements FSVisitor {
    ChecksumCalculator calc;
//    static private final int internalBufferSize = 16384;
//    static private final int chunkSize = 4096;

    public HashStreamWriter(ChecksumCalculator calc) {
        this.calc = calc;
    }

    @Override
    public void visit(FileNode node) {
        File f = new File(node.getPath());

        try {
            IO.println(calc.calculate(new FileInputStream(f)));
        } catch (Exception e) {
            // TODO:
        }
    }

    @Override
    public void visit(DirNode node) {
        for (FSNode fn : node.getChildren()) {
            fn.accept(this);
        }
    }
}
