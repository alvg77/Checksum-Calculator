package com.ChecksumCalculator.filesystem.visitor;

import com.ChecksumCalculator.filesystem.node.DirNode;
import com.ChecksumCalculator.filesystem.node.FSNode;
import com.ChecksumCalculator.filesystem.node.FileNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReportWriterTest {

    private ByteArrayOutputStream outputStream;
    private ReportWriter writer;

    @BeforeEach
    void setUp() {
        outputStream = new ByteArrayOutputStream();
        writer = new ReportWriter(outputStream);
    }

    @Test
    void visitFile_writesFilePathAndSize() {
        FileNode node = createMockFileNode("/path/to/file.txt", 1024L);

        writer.visit(node);

        String output = outputStream.toString();
        assertTrue(output.contains("File: /path/to/file.txt"));
        assertTrue(output.contains("Size: 1024 bytes"));
    }

    @Test
    void visitFile_handlesZeroByteFile() {
        FileNode node = createMockFileNode("/empty.txt", 0L);

        writer.visit(node);

        String output = outputStream.toString();
        assertTrue(output.contains("Size: 0 bytes"));
    }

    @Test
    void visitFile_handlesLargeFileSize() {
        FileNode node = createMockFileNode("/large.bin", 10_000_000_000L);

        writer.visit(node);

        String output = outputStream.toString();
        assertTrue(output.contains("Size: 10000000000 bytes"));
    }

    @Test
    void visitFile_handlesPathsWithSpecialCharacters() {
        FileNode node = createMockFileNode("/path/with spaces/file-name_v2.txt", 100L);

        writer.visit(node);

        String output = outputStream.toString();
        assertTrue(output.contains("/path/with spaces/file-name_v2.txt"));
    }

    @Test
    void visitDir_writesDirectoryPathAndTotalSize() {
        DirNode node = createMockDirNode("/path/to/directory", 5000L, List.of());

        writer.visit(node);

        String output = outputStream.toString();
        assertTrue(output.contains("Directory: /path/to/directory"));
        assertTrue(output.contains("Total Size: 5000 bytes"));
    }

    @Test
    void visitDir_visitsAllChildren() {
        FileNode child1 = createMockFileNode("/dir/file1.txt", 100L);
        FileNode child2 = createMockFileNode("/dir/file2.txt", 200L);
        DirNode node = createMockDirNode("/dir", 300L, List.of(child1, child2));

        writer.visit(node);

        String output = outputStream.toString();
        assertTrue(output.contains("Directory: /dir"));
        assertTrue(output.contains("File: /dir/file1.txt"));
        assertTrue(output.contains("File: /dir/file2.txt"));
    }

    @Test
    void visitDir_handlesNestedDirectories() {
        FileNode file = createMockFileNode("/outer/inner/file.txt", 100L);
        DirNode innerDir = createMockDirNode("/outer/inner", 100L, List.of(file));
        DirNode outerDir = createMockDirNode("/outer", 100L, List.of(innerDir));

        writer.visit(outerDir);

        String output = outputStream.toString();
        assertTrue(output.contains("Directory: /outer"));
        assertTrue(output.contains("Directory: /outer/inner"));
        assertTrue(output.contains("File: /outer/inner/file.txt"));
    }

    @Test
    void visitDir_handlesEmptyDirectory() {
        DirNode node = createMockDirNode("/empty", 0L, List.of());

        writer.visit(node);

        String output = outputStream.toString();
        assertTrue(output.contains("Directory: /empty"));
        assertTrue(output.contains("Total Size: 0 bytes"));
    }

    @Test
    void output_usesCorrectFormatForFiles() {
        FileNode node = createMockFileNode("/test.txt", 500L);

        writer.visit(node);

        String output = outputStream.toString();
        String[] lines = output.split("\n");

        assertTrue(lines[0].startsWith("File: "));
        assertTrue(lines[1].startsWith("Size: "));
        assertTrue(lines[1].endsWith("bytes"));
    }

    @Test
    void output_usesCorrectFormatForDirectories() {
        DirNode node = createMockDirNode("/testdir", 1000L, List.of());

        writer.visit(node);

        String output = outputStream.toString();
        String[] lines = output.split("\n");

        assertTrue(lines[0].startsWith("Directory: "));
        assertTrue(lines[1].startsWith("Total Size: "));
        assertTrue(lines[1].endsWith("bytes"));
    }

    private FileNode createMockFileNode(String path, long size) {
        FileNode node = mock(FileNode.class);
        when(node.getPath()).thenReturn(path);
        when(node.getSizeInBytes()).thenReturn(size);
        doAnswer(inv -> {
            FSVisitor v = inv.getArgument(0);
            v.visit(node);
            return null;
        }).when(node).accept(any());
        return node;
    }

    private DirNode createMockDirNode(String path, long size, List<FSNode> children) {
        DirNode node = mock(DirNode.class);
        when(node.getPath()).thenReturn(path);
        when(node.getSizeInBytes()).thenReturn(size);
        when(node.getChildren()).thenReturn(new java.util.ArrayList<>(children));
        doAnswer(inv -> {
            FSVisitor v = inv.getArgument(0);
            v.visit(node);
            return null;
        }).when(node).accept(any());
        return node;
    }
}