package com.ChecksumCalculator.filesystem.node;

import com.ChecksumCalculator.filesystem.visitor.FSVisitor;

public abstract class FSNode {
    private final String path;

    public FSNode(String path) {
        if (path == null) {
            throw new NullPointerException("FSNode path cannot be null");
        }

        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public abstract long getSizeInBytes();

    public abstract void accept(FSVisitor visitor);
}
