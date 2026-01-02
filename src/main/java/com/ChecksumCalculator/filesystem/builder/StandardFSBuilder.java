package com.ChecksumCalculator.filesystem.builder;

public class StandardFSBuilder extends AbstractFSBuilder {
    public StandardFSBuilder() {
        super();
    }

    @Override
    public FSBuilder buildAsSymlink() {
        return buildAsFile();
    }
}
