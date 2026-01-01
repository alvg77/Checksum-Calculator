package com.ChecksumCalculator.filesystem.builder;

import com.ChecksumCalculator.cli.CLIErrorHandler;
import com.ChecksumCalculator.filesystem.node.FSNode;
import mslinks.ShellLink;
import mslinks.ShellLinkException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
                    CLIErrorHandler.warn("Warning: Cycle detected, skipping: " + file.getAbsolutePath());
                    continue;
                }

                visitedPaths.add(realPath);
                children.add(build(file));
            } catch (IOException e) {
                CLIErrorHandler.fatal(e.getMessage(), CLIErrorHandler.ErrorType.IO);
            }
        }

        return children;
    }

    @Override
    protected FSNode buildSymlinkOrShortcut(File file) {
        if (Files.isSymbolicLink(file.toPath())) {
            try {
                Path linkPath = file.toPath().toRealPath();
                File target = linkPath.toFile();

                if (target.isDirectory()) {
                    return buildDirectory(target);
                } else {
                    return buildFile(target);
                }
            } catch (IOException e) {
                CLIErrorHandler.fatal("Cannot follow symlink: " + e.getMessage(),
                        CLIErrorHandler.ErrorType.IO);
            }
        }

        if (file.getName().toLowerCase().endsWith(".lnk")) {
            try {
                ShellLink link = new ShellLink(file);
                String targetPath = link.resolveTarget();
                File target = new File(targetPath);

                if (target.isDirectory()) {
                    return buildDirectory(target);
                } else {
                    return buildFile(target);
                }
            } catch (ShellLinkException | IOException e) {
                CLIErrorHandler.fatal("Cannot follow windows shortcut: " + e.getMessage(), CLIErrorHandler.ErrorType.IO); // TODO:
            }
        }

        return null;
    }
}
