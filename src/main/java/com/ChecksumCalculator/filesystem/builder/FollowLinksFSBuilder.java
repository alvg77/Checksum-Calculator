package com.ChecksumCalculator.filesystem.builder;

import com.ChecksumCalculator.filesystem.node.FSNode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FollowLinksFSBuilder extends FSBuilder {
    private final Set<String> visitedPaths;

    public FollowLinksFSBuilder() {
        this.visitedPaths = new HashSet<>();
    }

    @Override
    protected List<FSNode> buildDirectoryChildren(File[] files) {
        List<FSNode> children = new ArrayList<>();

        for (File file : files) {
            try {
                String realPath = file.toPath().toRealPath().toString();

                if (visitedPaths.contains(realPath)) {
                    System.err.println("Warning: Cycle detected, skipping: " + file.getAbsolutePath());
                    continue;
                }

                visitedPaths.add(realPath);
                children.add(build(file));
            } catch (IOException e) {
                throw new RuntimeException("Cannot resolve path: " + file.getAbsolutePath(), e);
            }
        }

        return children;
    }

    @Override
    protected FSNode buildSymlinkOrShortcut(File file) {
        try {
            Path linkPath = file.toPath().toRealPath();
            File target = linkPath.toFile();

            if (target.isDirectory()) {
                return buildDirectory(file);
            } else {
                return buildFile(file);
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot follow symlink/shortcut: " + e.getMessage());
        }
    }
}
