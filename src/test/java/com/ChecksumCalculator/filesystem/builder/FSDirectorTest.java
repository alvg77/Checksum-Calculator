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

public class FSDirectorTest {

    private FSDirector director;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        StandardFSBuilder builder = new StandardFSBuilder();
        director = new FSDirector(builder);
    }

    @Test
    void construct_createsFileNodeForFile() throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "hello");

        FSNode result = director.construct(file.toString());

        assertInstanceOf(FileNode.class, result);
        assertEquals(file.toString(), result.getPath());
        assertEquals(5, result.getSizeInBytes());
    }

    @Test
    void construct_createsDirNodeForDirectory() throws IOException {
        Path dir = tempDir.resolve("subdir");
        Files.createDirectory(dir);

        FSNode result = director.construct(dir.toString());

        assertInstanceOf(DirNode.class, result);
        assertEquals(dir.toString(), result.getPath());
    }

    @Test
    void construct_throwsForNonExistentPath() {
        String nonExistent = tempDir.resolve("nonexistent").toString();

        FileSystemException exception = assertThrows(FileSystemException.class,
                () -> director.construct(nonExistent));

        assertTrue(exception.getMessage().contains("does not exist"));
    }


    @Test
    void construct_includesFilesInDirectory() throws IOException {
        Path dir = tempDir.resolve("subdir");
        Files.createDirectory(dir);
        Path file1 = dir.resolve("file1.txt");
        Path file2 = dir.resolve("file2.txt");
        Files.writeString(file1, "content1");
        Files.writeString(file2, "content2");

        FSNode result = director.construct(dir.toString());

        DirNode dirNode = (DirNode) result;
        assertEquals(2, dirNode.getChildren().size());
    }

    @Test
    void construct_handlesNestedDirectories() throws IOException {
        Path parent = tempDir.resolve("parent");
        Path child = parent.resolve("child");
        Path grandchild = child.resolve("grandchild");
        Files.createDirectories(grandchild);
        Files.writeString(grandchild.resolve("file.txt"), "content");

        FSNode result = director.construct(parent.toString());

        DirNode parentNode = (DirNode) result;
        assertEquals(1, parentNode.getChildren().size());

        DirNode childNode = (DirNode) parentNode.getChildren().getFirst();
        assertEquals(1, childNode.getChildren().size());

        DirNode grandchildNode = (DirNode) childNode.getChildren().getFirst();
        assertEquals(1, grandchildNode.getChildren().size());
    }

    @Test
    void construct_handlesEmptyDirectory() throws IOException {
        Path emptyDir = tempDir.resolve("empty");
        Files.createDirectory(emptyDir);

        FSNode result = director.construct(emptyDir.toString());

        DirNode dirNode = (DirNode) result;
        assertTrue(dirNode.getChildren().isEmpty());
    }

    @Test
    void construct_calculatesCorrectTotalSize() throws IOException {
        Path dir = tempDir.resolve("subdir");
        Files.createDirectory(dir);
        Files.writeString(dir.resolve("file1.txt"), "12345");
        Files.writeString(dir.resolve("file2.txt"), "1234567890");

        FSNode result = director.construct(dir.toString());

        assertEquals(15, result.getSizeInBytes());
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void construct_withStandardBuilder_treatsSymlinkAsFile() throws IOException {
        Path target = tempDir.resolve("target.txt");
        Path link = tempDir.resolve("link.txt");
        Files.writeString(target, "content");
        Files.createSymbolicLink(link, target);

        FSNode result = director.construct(link.toString());

        assertInstanceOf(FileNode.class, result);
        assertEquals(link.toString(), result.getPath());
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void construct_withFollowLinksBuilder_resolvesSymlinkToFile() throws IOException {
        Path target = tempDir.resolve("target.txt");
        Path link = tempDir.resolve("link.txt");
        Files.writeString(target, "content");
        Files.createSymbolicLink(link, target);

        director.setBuilder(new FollowLinksFSBuilder());
        FSNode result = director.construct(link.toString());

        assertInstanceOf(FileNode.class, result);
        assertEquals(target.toString(), result.getPath());
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void construct_withFollowLinksBuilder_resolvesSymlinkToDirectory() throws IOException {
        Path targetDir = tempDir.resolve("targetDir");
        Path link = tempDir.resolve("linkDir");
        Files.createDirectory(targetDir);
        Files.writeString(targetDir.resolve("file.txt"), "content");
        Files.createSymbolicLink(link, targetDir);

        director.setBuilder(new FollowLinksFSBuilder());
        FSNode result = director.construct(link.toString());

        assertInstanceOf(DirNode.class, result);
        DirNode dirNode = (DirNode) result;
        assertEquals(1, dirNode.getChildren().size());
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void construct_withFollowLinksBuilder_detectsSimpleCycle() throws IOException {
        Path dir1 = tempDir.resolve("dir1");
        Path dir2 = tempDir.resolve("dir2");
        Files.createDirectory(dir1);
        Files.createDirectory(dir2);
        Files.createSymbolicLink(dir1.resolve("link"), dir2);
        Files.createSymbolicLink(dir2.resolve("link"), dir1);

        director.setBuilder(new FollowLinksFSBuilder());

        FSNode result = director.construct(dir1.toString());

        assertNotNull(result);
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void construct_withFollowLinksBuilder_detectsSelfReference() throws IOException {
        Path dir = tempDir.resolve("selfref");
        Files.createDirectory(dir);
        Files.createSymbolicLink(dir.resolve("link"), dir);

        director.setBuilder(new FollowLinksFSBuilder());

        FSNode result = director.construct(dir.toString());

        assertNotNull(result);
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void setBuilder_changesBuilderUsed() throws IOException {
        Path target = tempDir.resolve("target.txt");
        Path link = tempDir.resolve("link.txt");
        Files.writeString(target, "content");
        Files.createSymbolicLink(link, target);

        FSNode result1 = director.construct(link.toString());
        assertEquals(link.toString(), result1.getPath());

        director.setBuilder(new FollowLinksFSBuilder());
        FSNode result2 = director.construct(link.toString());
        assertEquals(target.toString(), result2.getPath());
    }

    @Test
    void construct_handlesMixedContent() throws IOException {
        Path dir = tempDir.resolve("mixed");
        Files.createDirectory(dir);
        Files.writeString(dir.resolve("file.txt"), "content");
        Files.createDirectory(dir.resolve("subdir"));
        Files.writeString(dir.resolve("subdir").resolve("nested.txt"), "nested");

        FSNode result = director.construct(dir.toString());

        DirNode dirNode = (DirNode) result;
        assertEquals(2, dirNode.getChildren().size());
    }

    @Test
    void construct_handlesZeroByteFile() throws IOException {
        Path emptyFile = tempDir.resolve("empty.txt");
        Files.createFile(emptyFile);

        FSNode result = director.construct(emptyFile.toString());

        assertEquals(0, result.getSizeInBytes());
    }

    @Test
    void construct_handlesSpecialCharactersInPath() throws IOException {
        Path specialDir = tempDir.resolve("special dir (1)");
        Files.createDirectory(specialDir);
        Files.writeString(specialDir.resolve("file [2].txt"), "content");

        FSNode result = director.construct(specialDir.toString());

        assertNotNull(result);
        assertEquals(specialDir.toString(), result.getPath());
    }
}
