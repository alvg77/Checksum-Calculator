package com.ChecksumCalculator.filesystem.node;

import com.ChecksumCalculator.filesystem.visitor.FSVisitor;

import java.util.List;

public class DirNode extends FSNode {
    List<FSNode> children;

    public DirNode(String path, List<FSNode> children) {
        super(path);
        this.children = children;
    }

    @Override
    public long getSizeInBytes() {
        long total = 0;

        for (FSNode child : children) {
            total += child.getSizeInBytes();
        }

        return total;
    }

    @Override
    public void accept(FSVisitor visitor) {
        visitor.visit(this);
    }

    public List<FSNode> getChildren() {
        return children;
    }
}
