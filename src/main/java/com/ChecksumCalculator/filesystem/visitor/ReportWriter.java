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
        writer.println("File: " + node.getPath());
        writer.println("Size: " + node.getSizeInBytes() + " bytes");
    }

    @Override
    public void visit(DirNode node) {
        writer.println("Directory: " + node.getPath());
        writer.println("Total Size: " + node.getSizeInBytes() + " bytes");

        for (FSNode child : node.getChildren()) {
            child.accept(this);
        }
    }
}