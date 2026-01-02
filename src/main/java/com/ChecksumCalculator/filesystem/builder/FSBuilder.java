package com.ChecksumCalculator.filesystem.builder;

import com.ChecksumCalculator.filesystem.node.FSNode;

import java.io.File;
import java.util.List;

public interface FSBuilder {
    FSBuilder reset();

    FSBuilder setFile(File file);

    FSBuilder buildAsFile();

    FSBuilder buildAsDirectory();

    FSBuilder buildAsSymlink();

    FSBuilder addChildren(List<FSNode> children);

    FSNode getResult();

    boolean isSymlinkOrShortcut();

    boolean isFile();

    boolean isDirectory();
}
