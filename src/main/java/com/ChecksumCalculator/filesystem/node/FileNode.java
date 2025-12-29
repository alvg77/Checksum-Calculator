package com.ChecksumCalculator.filesystem.node;

import com.ChecksumCalculator.filesystem.visitor.FSVisitor;

public class FileNode extends FSNode {
    final long sizeInBytes;

    public FileNode(String path, long sizeInBytes) {
        super(path);
        this.sizeInBytes = sizeInBytes;
    }

    public long getSizeInBytes() {
        return sizeInBytes;
    }

    @Override
    public void accept(FSVisitor visitor) {
        visitor.visit(this);
    }
}
