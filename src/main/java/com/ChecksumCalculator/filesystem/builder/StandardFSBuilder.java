package com.ChecksumCalculator.filesystem.builder;

import com.ChecksumCalculator.filesystem.node.FSNode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class StandardFSBuilder extends FSBuilder {
    @Override
    protected List<FSNode> buildDirectoryChildren(File[] files) {
        List<FSNode> children = new ArrayList<>();

        for (File file : files) {
            children.add(build(file));
        }

        return children;
    }

    @Override
    protected FSNode buildSymlinkOrShortcut(File file) {
        return buildFile(file);
    }
}
