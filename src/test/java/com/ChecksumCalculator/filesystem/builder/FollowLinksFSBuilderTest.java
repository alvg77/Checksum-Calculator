package com.ChecksumCalculator.filesystem.builder;

import com.ChecksumCalculator.exception.FileSystemException;
import com.ChecksumCalculator.filesystem.node.DirNode;
import com.ChecksumCalculator.filesystem.node.FSNode;
import com.ChecksumCalculator.filesystem.node.FileNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class FollowLinksFSBuilderTest {

    private FollowLinksFSBuilder builder;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        builder = new FollowLinksFSBuilder();
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void buildAsSymlink_resolvesSymlinkToFile() throws IOException {
        Path target = tempDir.resolve("target.txt");
        Path link = tempDir.resolve("link.txt");
        Files.writeString(target, "content");
        Files.createSymbolicLink(link, target);

        FSNode result = builder.setFile(link.toFile()).buildAsSymlink().getResult();

        assertInstanceOf(FileNode.class, result);
        assertEquals(target.toString(), result.getPath());
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void buildAsSymlink_resolvesSymlinkToDirectory() throws IOException {
        Path targetDir = tempDir.resolve("targetDir");
        Path link = tempDir.resolve("linkDir");
        Files.createDirectory(targetDir);
        Files.createSymbolicLink(link, targetDir);

        FSNode result = builder.setFile(link.toFile()).buildAsSymlink().getResult();

        assertInstanceOf(DirNode.class, result);
        assertEquals(targetDir.toString(), result.getPath());
    }

    @Test
    void buildAsSymlink_throwsWhenFileNotSet() {
        assertThrows(IllegalStateException.class, () -> builder.buildAsSymlink());
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void buildAsSymlink_throwsForBrokenSymlink() throws IOException {
        Path target = tempDir.resolve("nonexistent.txt");
        Path link = tempDir.resolve("broken_link.txt");
        Files.createSymbolicLink(link, target);

        builder.setFile(link.toFile());

        assertThrows(FileSystemException.class, () -> builder.buildAsSymlink());
    }

    @Test
    void buildAsSymlink_returnsNullResultForNonLink() throws IOException {
        Path file = tempDir.resolve("regular.txt");
        Files.writeString(file, "content");

        builder.setFile(file.toFile()).buildAsSymlink();
        FSNode result = builder.getResult();

        assertNull(result);
    }

    @Test
    void hasVisited_returnsFalseForUnvisitedFile() throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "content");

        assertFalse(builder.hasVisited(file.toFile()));
    }

    @Test
    void hasVisited_returnsTrueAfterMarkVisited() throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "content");

        builder.markVisited(file.toFile());

        assertTrue(builder.hasVisited(file.toFile()));
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void hasVisited_detectsVisitedSymlinkTarget() throws IOException {
        Path target = tempDir.resolve("target.txt");
        Path link = tempDir.resolve("link.txt");
        Files.writeString(target, "content");
        Files.createSymbolicLink(link, target);

        builder.markVisited(target.toFile());

        assertTrue(builder.hasVisited(link.toFile()));
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void hasVisited_detectsVisitedThroughDifferentSymlinks() throws IOException {
        Path target = tempDir.resolve("target.txt");
        Path link1 = tempDir.resolve("link1.txt");
        Path link2 = tempDir.resolve("link2.txt");
        Files.writeString(target, "content");
        Files.createSymbolicLink(link1, target);
        Files.createSymbolicLink(link2, target);

        builder.markVisited(link1.toFile());

        assertTrue(builder.hasVisited(link2.toFile()));
    }

    @Test
    void hasVisited_returnsFalseForNonExistentFile() {
        Path nonExistent = tempDir.resolve("nonexistent.txt");

        assertFalse(builder.hasVisited(nonExistent.toFile()));
    }

    @Test
    void markVisited_addsFileToVisitedSet() throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "content");

        builder.markVisited(file.toFile());

        assertTrue(builder.hasVisited(file.toFile()));
    }

    @Test
    void markVisited_throwsForNonExistentFile() {
        Path nonExistent = tempDir.resolve("nonexistent.txt");

        assertThrows(FileSystemException.class, () -> builder.markVisited(nonExistent.toFile()));
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void markVisited_resolvesSymlinksBeforeStoring() throws IOException {
        Path target = tempDir.resolve("target.txt");
        Path link = tempDir.resolve("link.txt");
        Files.writeString(target, "content");
        Files.createSymbolicLink(link, target);

        builder.markVisited(link.toFile());

        assertTrue(builder.hasVisited(target.toFile()));
    }

    @Test
    void buildAsFile_worksAsInherited() throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "hello");

        FSNode result = builder.setFile(file.toFile()).buildAsFile().getResult();

        assertInstanceOf(FileNode.class, result);
        assertEquals(file.toString(), result.getPath());
        assertEquals(5, result.getSizeInBytes());
    }

    @Test
    void buildAsDirectory_worksAsInherited() throws IOException {
        Path dir = tempDir.resolve("subdir");
        Files.createDirectory(dir);

        FSNode result = builder.setFile(dir.toFile()).buildAsDirectory().getResult();

        assertInstanceOf(DirNode.class, result);
        assertEquals(dir.toString(), result.getPath());
    }
}