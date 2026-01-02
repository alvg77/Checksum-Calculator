package com.ChecksumCalculator.filesystem.builder;

import com.ChecksumCalculator.cli.CLIErrorHandler;
import com.ChecksumCalculator.filesystem.node.FSNode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FSDirector {
    private FSBuilder builder;

    public FSDirector(FSBuilder builder) {
        this.builder = builder;
    }

    public void setBuilder(FSBuilder builder) {
        this.builder = builder;
    }

    public FSNode construct(String rootPath) {
        File file = new File(rootPath);

        if (!file.exists()) {
//            TODO:
            System.exit(111);
        }

        return constructNode(file);
    }

    private FSNode constructNode(File file) {
        builder.reset().setFile(file);

        if (builder.isSymlinkOrShortcut()) {
            return constructSymlink(file);
        }

        if (builder.isFile()) {
            return constructFile();
        }

        if (builder.isDirectory()) {
            return constructDirectory(file);
        }

        throw new RuntimeException("Unsupported file type: " + file.getPath());
    }

    private FSNode constructFile() {
        return builder.buildAsFile().getResult();
    }

    private FSNode constructDirectory(File file) {
        File[] files = file.listFiles();

        if (files == null) {
            throw new RuntimeException("Cannot read directory: " + file.getPath()
                    + "! Please check your permissions.");
        }

        List<FSNode> children = constructChildren(files);

        return builder
                .setFile(file)
                .addChildren(children)
                .buildAsDirectory()
                .getResult();
    }

    private List<FSNode> constructChildren(File[] files) {
        List<FSNode> children = new ArrayList<>();

        for (File childFile : files) {
            if (shouldSkipFile(childFile)) {
                continue;
            }

            markFileAsVisited(childFile);
            FSNode childNode = constructNode(childFile);

            if (childNode != null) {
                children.add(childNode);
            }
        }

        return children;
    }

    private boolean shouldSkipFile(File file) {
        if (builder instanceof FollowLinksFSBuilder followBuilder) {
            if (followBuilder.hasVisited(file)) {
                CLIErrorHandler.warn("Cycle detected, skipping: " + file.getAbsolutePath());
                return true;
            }
        }
        return false;
    }

    private void markFileAsVisited(File file) {
        if (builder instanceof FollowLinksFSBuilder followBuilder) {
            followBuilder.markVisited(file);
        }
    }

    private FSNode constructSymlink(File file) {
        if (builder instanceof FollowLinksFSBuilder) {
            builder.buildAsSymlink();

            if (builder.isDirectory()) {
                return constructDirectory(file);
            }

            return builder.getResult();
        } else {
            return builder.buildAsSymlink().getResult();
        }
    }
}
