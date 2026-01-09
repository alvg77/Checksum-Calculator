package com.ChecksumCalculator.filesystem.builder;

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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class StandardFSBuilderTest {

    private StandardFSBuilder builder;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        builder = new StandardFSBuilder();
    }

    @Test
    void reset_clearsCurrentFile() throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "content");

        builder.setFile(file.toFile());
        builder.reset();

        assertThrows(IllegalStateException.class, () -> builder.buildAsFile());
    }

    @Test
    void reset_returnsBuilderForChaining() {
        FSBuilder result = builder.reset();

        assertSame(builder, result);
    }

    @Test
    void setFile_storesFileReference() throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "hello");

        builder.setFile(file.toFile());

        assertTrue(builder.isFile());
    }

    @Test
    void setFile_returnsBuilderForChaining() throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "content");

        FSBuilder result = builder.setFile(file.toFile());

        assertSame(builder, result);
    }

    @Test
    void buildAsFile_createsFileNodeWithCorrectPath() throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "hello");

        FSNode result = builder.setFile(file.toFile()).buildAsFile().getResult();

        assertInstanceOf(FileNode.class, result);
        assertEquals(file.toString(), result.getPath());
    }

    @Test
    void buildAsFile_createsFileNodeWithCorrectSize() throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "hello"); // 5 bytes

        FSNode result = builder.setFile(file.toFile()).buildAsFile().getResult();

        assertEquals(5, result.getSizeInBytes());
    }

    @Test
    void buildAsFile_throwsWhenFileNotSet() {
        assertThrows(IllegalStateException.class, () -> builder.buildAsFile());
    }

    @Test
    void buildAsFile_returnsBuilderForChaining() throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "content");

        FSBuilder result = builder.setFile(file.toFile()).buildAsFile();

        assertSame(builder, result);
    }

    @Test
    void buildAsDirectory_createsDirNodeWithCorrectPath() throws IOException {
        Path dir = tempDir.resolve("subdir");
        Files.createDirectory(dir);

        FSNode result = builder.setFile(dir.toFile()).buildAsDirectory().getResult();

        assertInstanceOf(DirNode.class, result);
        assertEquals(dir.toString(), result.getPath());
    }

    @Test
    void buildAsDirectory_includesAddedChildren() throws IOException {
        Path dir = tempDir.resolve("subdir");
        Files.createDirectory(dir);

        FileNode child1 = new FileNode("/child1.txt", 100);
        FileNode child2 = new FileNode("/child2.txt", 200);

        FSNode result = builder
                .setFile(dir.toFile())
                .addChildren(List.of(child1, child2))
                .buildAsDirectory()
                .getResult();

        DirNode dirNode = (DirNode) result;
        assertEquals(2, dirNode.getChildren().size());
    }

    @Test
    void buildAsDirectory_throwsWhenFileNotSet() {
        assertThrows(IllegalStateException.class, () -> builder.buildAsDirectory());
    }

    @Test
    void buildAsDirectory_hasEmptyChildrenByDefault() throws IOException {
        Path dir = tempDir.resolve("subdir");
        Files.createDirectory(dir);

        FSNode result = builder.setFile(dir.toFile()).buildAsDirectory().getResult();

        DirNode dirNode = (DirNode) result;
        assertTrue(dirNode.getChildren().isEmpty());
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void buildAsSymlink_treatsSymlinkAsFile() throws IOException {
        Path target = tempDir.resolve("target.txt");
        Path link = tempDir.resolve("link.txt");
        Files.writeString(target, "content");
        Files.createSymbolicLink(link, target);

        FSNode result = builder.setFile(link.toFile()).buildAsSymlink().getResult();

        assertInstanceOf(FileNode.class, result);
        assertEquals(link.toString(), result.getPath());
    }

    @Test
    void addChildren_storesChildrenForDirectory() throws IOException {
        Path dir = tempDir.resolve("subdir");
        Files.createDirectory(dir);

        FileNode child = new FileNode("/child.txt", 50);

        builder.setFile(dir.toFile()).addChildren(List.of(child)).buildAsDirectory();
        DirNode result = (DirNode) builder.getResult();

        assertEquals(1, result.getChildren().size());
        assertEquals(child, result.getChildren().getFirst());
    }

    @Test
    void addChildren_makesDefensiveCopy() throws IOException {
        Path dir = tempDir.resolve("subdir");
        Files.createDirectory(dir);

        List<FSNode> children = new java.util.ArrayList<>();
        children.add(new FileNode("/child.txt", 50));

        builder.setFile(dir.toFile()).addChildren(children);
        children.clear();

        builder.buildAsDirectory();
        DirNode result = (DirNode) builder.getResult();

        assertEquals(1, result.getChildren().size());
    }

    @Test
    void getResult_returnsBuiltNode() throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "content");

        builder.setFile(file.toFile()).buildAsFile();
        FSNode result = builder.getResult();

        assertNotNull(result);
        assertEquals(file.toString(), result.getPath());
    }

    @Test
    void getResult_resetsBuilderAfterCall() throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "content");

        builder.setFile(file.toFile()).buildAsFile().getResult();

        assertThrows(IllegalStateException.class, () -> builder.buildAsFile());
    }

    @Test
    void getResult_returnsNullIfNothingBuilt() {
        FSNode result = builder.getResult();

        assertNull(result);
    }

    @Test
    void isFile_returnsTrueForRegularFile() throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "content");

        builder.setFile(file.toFile());

        assertTrue(builder.isFile());
    }

    @Test
    void isFile_returnsFalseForDirectory() throws IOException {
        Path dir = tempDir.resolve("subdir");
        Files.createDirectory(dir);

        builder.setFile(dir.toFile());

        assertFalse(builder.isFile());
    }

    @Test
    void isFile_returnsFalseWhenNoFileSet() {
        assertFalse(builder.isFile());
    }

    @Test
    void isDirectory_returnsTrueForDirectory() throws IOException {
        Path dir = tempDir.resolve("subdir");
        Files.createDirectory(dir);

        builder.setFile(dir.toFile());

        assertTrue(builder.isDirectory());
    }

    @Test
    void isDirectory_returnsFalseForRegularFile() throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "content");

        builder.setFile(file.toFile());

        assertFalse(builder.isDirectory());
    }

    @Test
    void isDirectory_returnsFalseWhenNoFileSet() {
        assertFalse(builder.isDirectory());
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void isSymlinkOrShortcut_returnsTrueForSymlink() throws IOException {
        Path target = tempDir.resolve("target.txt");
        Path link = tempDir.resolve("link.txt");
        Files.writeString(target, "content");
        Files.createSymbolicLink(link, target);

        builder.setFile(link.toFile());

        assertTrue(builder.isSymlinkOrShortcut());
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void isSymlinkOrShortcut_returnsTrueForLnkFile() throws IOException {
        Path lnkFile = tempDir.resolve("shortcut.lnk");
        Files.writeString(lnkFile, "fake content");

        builder.setFile(lnkFile.toFile());

        assertTrue(builder.isSymlinkOrShortcut());
    }

    @Test
    void isSymlinkOrShortcut_returnsFalseForRegularFile() throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "content");

        builder.setFile(file.toFile());

        assertFalse(builder.isSymlinkOrShortcut());
    }

    @Test
    void isSymlinkOrShortcut_returnsFalseWhenNoFileSet() {
        assertFalse(builder.isSymlinkOrShortcut());
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void isSymlinkOrShortcut_caseInsensitiveForLnk() throws IOException {
        Path lnkFile = tempDir.resolve("shortcut.LNK");
        Files.writeString(lnkFile, "fake content");

        builder.setFile(lnkFile.toFile());

        assertTrue(builder.isSymlinkOrShortcut());
    }
}
