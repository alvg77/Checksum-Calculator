package com.ChecksumCalculator.filesystem.node;

import com.ChecksumCalculator.filesystem.visitor.FSVisitor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class FileNodeTest {

    @Test
    void constructor_setsPathAndSize() {
        FileNode node = new FileNode("/path/to/file.txt", 1024);

        assertEquals("/path/to/file.txt", node.getPath());
        assertEquals(1024, node.getSizeInBytes());
    }

    @Test
    void constructor_throwsNullPointerException_whenPathIsNull() {
        assertThrows(NullPointerException.class, () -> new FileNode(null, 100));
    }

    @Test
    void constructor_acceptsZeroSize() {
        FileNode node = new FileNode("/empty.txt", 0);

        assertEquals(0, node.getSizeInBytes());
    }

    @Test
    void accept_callsVisitorWithSelf() {
        FileNode node = new FileNode("/file.txt", 100);
        FSVisitor visitor = mock(FSVisitor.class);

        node.accept(visitor);

        verify(visitor).visit(node);
    }
}