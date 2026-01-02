package com.ChecksumCalculator.filesystem.builder;

import com.ChecksumCalculator.filesystem.node.FileNode;

public class StandardFSBuilder extends AbstractFSBuilder {
    public StandardFSBuilder() {
        super();
    }

    @Override
    public FSBuilder buildAsSymlink() {
        return buildAsFile();
    }
}
