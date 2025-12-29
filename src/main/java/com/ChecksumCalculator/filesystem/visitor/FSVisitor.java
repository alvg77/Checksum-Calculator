package com.ChecksumCalculator.filesystem.visitor;

import com.ChecksumCalculator.filesystem.node.DirNode;
import com.ChecksumCalculator.filesystem.node.FileNode;

public interface FSVisitor {
    void visit(FileNode node);
    void visit(DirNode node);
}
