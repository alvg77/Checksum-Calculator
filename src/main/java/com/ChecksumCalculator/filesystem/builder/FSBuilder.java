package com.ChecksumCalculator.filesystem.builder;

import com.ChecksumCalculator.filesystem.node.DirNode;
import com.ChecksumCalculator.filesystem.node.FSNode;
import com.ChecksumCalculator.filesystem.node.FileNode;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public abstract class FSBuilder {
    public FSNode build(String rootPath) {
        File file = new File(rootPath);

        if (!file.exists()) {
            throw new RuntimeException("Path does not exist: " + file.getPath());
        }

        return build(file);
    }

    protected FSNode build(File file) {
        if (isSymlinkOrShortcut(file.toPath())) {
            return buildSymlinkOrShortcut(file);
        }

        if (file.isFile()) {
            return buildFile(file);
        }

        if (file.isDirectory()) {
            return buildDirectory(file);
        }

        throw new RuntimeException("Unsupported file type: " + file.getPath());
    }

    protected FSNode buildFile(File file) {
        return new FileNode(file.getPath(), file.length());
    }

    protected FSNode buildDirectory(File file) {
        File[] files = file.listFiles();

        if (files == null) {
            throw new RuntimeException("Cannot read directory: " + file.getPath() + "! Please check your permissions.");
        }

        List<FSNode> children = buildDirectoryChildren(files);

        return new DirNode(file.getPath(), children);
    }

    protected abstract List<FSNode> buildDirectoryChildren(File[] files);

    protected abstract FSNode buildSymlinkOrShortcut(File file);

    protected static boolean isSymlinkOrShortcut(Path path) {
        if (Files.isSymbolicLink(path)) {
            return true;
        }

        return path.toString().toLowerCase().endsWith(".lnk");
    }
}
