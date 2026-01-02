package com.ChecksumCalculator.filesystem.builder;

import com.ChecksumCalculator.filesystem.node.DirNode;
import com.ChecksumCalculator.filesystem.node.FSNode;
import com.ChecksumCalculator.filesystem.node.FileNode;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractFSBuilder implements FSBuilder {
    protected File currentFile;
    private FSNode result;
    private List<FSNode> children;

    public AbstractFSBuilder() {
        reset();
    }

    @Override
    public FSBuilder reset() {
        this.currentFile = null;
        this.result = null;
        this.children = new ArrayList<>();
        return this;
    }

    @Override
    public FSBuilder setFile(File file) {
        this.currentFile = file;
        return this;
    }

    @Override
    public FSBuilder buildAsFile() {
        if (currentFile == null) {
            throw new IllegalStateException("File not set");
        }
        this.result = new FileNode(currentFile.getPath(), currentFile.length());
        return this;
    }

    @Override
    public FSBuilder buildAsDirectory() {
        if (currentFile == null) {
            throw new IllegalStateException("File not set");
        }
        this.result = new DirNode(currentFile.getPath(), children);
        return this;
    }

    @Override
    public FSBuilder addChildren(List<FSNode> children) {
        this.children = new ArrayList<>(children);
        return this;
    }

    @Override
    public FSNode getResult() {
        FSNode builtResult = this.result;
        reset();
        return builtResult;
    }

    @Override
    public boolean isSymlinkOrShortcut() {
        if (currentFile == null) return false;
        return Files.isSymbolicLink(currentFile.toPath())
                || currentFile.getName().toLowerCase().endsWith(".lnk");
    }

    @Override
    public boolean isDirectory() {
        return currentFile != null && currentFile.isDirectory();
    }

    @Override
    public boolean isFile() {
        return currentFile != null && currentFile.isFile();
    }
}
