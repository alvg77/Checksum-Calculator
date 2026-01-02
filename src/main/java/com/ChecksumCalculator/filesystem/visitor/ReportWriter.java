package com.ChecksumCalculator.filesystem.visitor;

import com.ChecksumCalculator.filesystem.node.DirNode;
import com.ChecksumCalculator.filesystem.node.FSNode;
import com.ChecksumCalculator.filesystem.node.FileNode;

import java.io.OutputStream;
import java.io.PrintWriter;

public class ReportWriter implements FSVisitor {
    private final PrintWriter writer;

    public ReportWriter(OutputStream os) {
        this.writer = new PrintWriter(os, true);
    }

    @Override
    public void visit(FileNode node) {
        writer.printf("File: %s\n", node.getPath());
        writer.printf("Size: %s bytes\n", node.getSizeInBytes());
    }

    @Override
    public void visit(DirNode node) {
        writer.printf("Directory: %s\n", node.getPath());
        writer.printf("Total Size: %s bytes\n", node.getSizeInBytes());

        for (FSNode child : node.getChildren()) {
            child.accept(this);
        }
    }
}