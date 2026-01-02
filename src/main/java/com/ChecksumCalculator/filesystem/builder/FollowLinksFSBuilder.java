package com.ChecksumCalculator.filesystem.builder;

import mslinks.ShellLink;
import mslinks.ShellLinkException;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class FollowLinksFSBuilder extends AbstractFSBuilder{
    private final Set<String> visitedPaths;

    public FollowLinksFSBuilder() {
        super();
        this.visitedPaths = new HashSet<>();
    }

    @Override
    public FSBuilder buildAsSymlink() {
        if (currentFile == null) {
            throw new IllegalStateException("File not set");
        }

        File resolvedTarget = resolveLink();
        if (resolvedTarget == null) {
            return this;
        }

        this.currentFile = resolvedTarget;

        if (resolvedTarget.isDirectory()) {
            return buildAsDirectory();
        } else {
            return buildAsFile();
        }
    }

    private File resolveLink() {
        if (Files.isSymbolicLink(currentFile.toPath())) {
            try {
                Path linkPath = currentFile.toPath().toRealPath();
                return linkPath.toFile();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        if (currentFile.getName().toLowerCase().endsWith(".lnk")) {
            try {
                ShellLink link = new ShellLink(currentFile);
                String targetPath = link.resolveTarget();
                return new File(targetPath);
            } catch (ShellLinkException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        return null;
    }

    public boolean hasVisited(File file) {
        try {
            String realPath = file.toPath().toRealPath().toString();
            return visitedPaths.contains(realPath);
        } catch (IOException e) {
            return false;
        }
    }

    public void markVisited(File file) {
        try {
            String realPath = file.toPath().toRealPath().toString();
            visitedPaths.add(realPath);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
