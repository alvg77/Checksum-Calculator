package com.ChecksumCalculator.filesystem.node;

import com.ChecksumCalculator.filesystem.visitor.FSVisitor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DirNodeTest {
    @Test
    void constructor_setsPathAndChildren() {
        List<FSNode> children = List.of(new FileNode("/dir/file.txt", 100));
        DirNode node = new DirNode("/dir", children);

        assertEquals("/dir", node.getPath());
        assertEquals(children, node.getChildren());
    }

    @Test
    void constructor_throwsNullPointerException_whenPathIsNull() {
        assertThrows(NullPointerException.class, () -> new DirNode(null, new ArrayList<>()));
    }

    @Test
    void getSizeInBytes_returnsZero_whenEmpty() {
        DirNode node = new DirNode("/empty", new ArrayList<>());

        assertEquals(0, node.getSizeInBytes());
    }

    @Test
    void getSizeInBytes_returnsSumOfChildrenSizes() {
        List<FSNode> children = List.of(
                new FileNode("/dir/file1.txt", 100),
                new FileNode("/dir/file2.txt", 250),
                new FileNode("/dir/file3.txt", 50)
        );
        DirNode node = new DirNode("/dir", children);

        assertEquals(400, node.getSizeInBytes());
    }

    @Test
    void getSizeInBytes_includesNestedDirectories() {
        DirNode nested = new DirNode("/dir/nested", List.of(
                new FileNode("/dir/nested/file.txt", 300)
        ));
        List<FSNode> children = List.of(
                new FileNode("/dir/file.txt", 100),
                nested
        );
        DirNode node = new DirNode("/dir", children);

        assertEquals(400, node.getSizeInBytes());
    }

    @Test
    void getSizeInBytes_handlesDeeplyNestedStructure() {
        DirNode level3 = new DirNode("/a/b/c", List.of(
                new FileNode("/a/b/c/file.txt", 50)
        ));
        DirNode level2 = new DirNode("/a/b", List.of(
                new FileNode("/a/b/file.txt", 100),
                level3
        ));
        DirNode level1 = new DirNode("/a", List.of(
                new FileNode("/a/file.txt", 200),
                level2
        ));

        assertEquals(350, level1.getSizeInBytes());
    }

    @Test
    void accept_callsVisitorWithSelf() {
        DirNode node = new DirNode("/dir", new ArrayList<>());
        FSVisitor visitor = mock(FSVisitor.class);

        node.accept(visitor);

        verify(visitor).visit(node);
    }

    @Test
    void getChildren_returnsChildrenList() {
        FileNode child1 = new FileNode("/dir/a.txt", 10);
        FileNode child2 = new FileNode("/dir/b.txt", 20);
        DirNode node = new DirNode("/dir", List.of(child1, child2));

        List<FSNode> children = node.getChildren();

        assertEquals(2, children.size());
        assertTrue(children.contains(child1));
        assertTrue(children.contains(child2));
    }
}