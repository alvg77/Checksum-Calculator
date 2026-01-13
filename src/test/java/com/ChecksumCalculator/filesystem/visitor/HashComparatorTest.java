package com.ChecksumCalculator.filesystem.visitor;

import com.ChecksumCalculator.calculator.ChecksumCalculator;
import com.ChecksumCalculator.filesystem.node.DirNode;
import com.ChecksumCalculator.filesystem.node.FileNode;
import com.ChecksumCalculator.io.reader.ChecksumReader;
import com.ChecksumCalculator.progress.observer.Observable;
import com.ChecksumCalculator.progress.observer.Observer;
import com.ChecksumCalculator.state.PauseMessage;
import com.ChecksumCalculator.state.memento.HashComparatorMemento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class HashComparatorTest {

    @TempDir
    Path tempDir;

    private ChecksumCalculator mockCalculator;
    private ChecksumReader mockReader;
    private HashComparator comparator;

    @BeforeEach
    void setUp() {
        mockCalculator = mock(ChecksumCalculator.class);
        mockReader = mock(ChecksumReader.class);
        when(mockReader.readHashes()).thenReturn(new HashMap<>());
    }

    @Test
    void constructor_registersAsObserverOfCalculator() {
        comparator = new HashComparator(mockReader, mockCalculator);

        verify(mockCalculator).addObserver(comparator);
    }

    @Test
    void compare_returnsOK_whenHashMatches() throws IOException {
        Path file = tempDir.resolve("file.txt");
        Files.writeString(file, "content");

        Map<String, String> existingHashes = new HashMap<>();
        existingHashes.put(file.toString(), "matching-hash");
        when(mockReader.readHashes()).thenReturn(existingHashes);
        when(mockCalculator.calculate(any())).thenReturn(Optional.of("matching-hash"));

        comparator = new HashComparator(mockReader, mockCalculator);
        FileNode node = createMockFileNode(file.toString());

        Map<String, Status> result = comparator.compare(node);

        assertEquals(Status.OK, result.get(file.toString()));
    }

    @Test
    void compare_returnsModified_whenHashDiffers() throws IOException {
        Path file = tempDir.resolve("file.txt");
        Files.writeString(file, "modified content");

        Map<String, String> existingHashes = new HashMap<>();
        existingHashes.put(file.toString(), "old-hash");
        when(mockReader.readHashes()).thenReturn(existingHashes);
        when(mockCalculator.calculate(any())).thenReturn(Optional.of("new-hash"));

        comparator = new HashComparator(mockReader, mockCalculator);
        FileNode node = createMockFileNode(file.toString());

        Map<String, Status> result = comparator.compare(node);

        assertEquals(Status.MODIFIED, result.get(file.toString()));
    }

    @Test
    void compare_returnsNew_whenFileNotInExistingHashes() throws IOException {
        Path file = tempDir.resolve("new-file.txt");
        Files.writeString(file, "new content");

        when(mockReader.readHashes()).thenReturn(new HashMap<>());
        when(mockCalculator.calculate(any())).thenReturn(Optional.of("some-hash"));

        comparator = new HashComparator(mockReader, mockCalculator);
        FileNode node = createMockFileNode(file.toString());

        Map<String, Status> result = comparator.compare(node);

        assertEquals(Status.NEW, result.get(file.toString()));
    }

    @Test
    void compare_returnsRemoved_forFilesInHashesButNotVisited() throws IOException {
        Path existingFile = tempDir.resolve("existing.txt");
        Files.writeString(existingFile, "content");

        Map<String, String> existingHashes = new HashMap<>();
        existingHashes.put(existingFile.toString(), "hash1");
        existingHashes.put("/removed/file.txt", "hash2");
        when(mockReader.readHashes()).thenReturn(existingHashes);
        when(mockCalculator.calculate(any())).thenReturn(Optional.of("hash1"));

        comparator = new HashComparator(mockReader, mockCalculator);
        FileNode node = createMockFileNode(existingFile.toString());

        Map<String, Status> result = comparator.compare(node);

        assertEquals(Status.OK, result.get(existingFile.toString()));
        assertEquals(Status.REMOVED, result.get("/removed/file.txt"));
    }

    @Test
    void compare_returnsNull_whenPaused() throws IOException {
        Path file = tempDir.resolve("file.txt");
        Files.writeString(file, "content");

        when(mockCalculator.calculate(any())).thenReturn(Optional.of("hash"));
        comparator = new HashComparator(mockReader, mockCalculator);
        comparator.update(mockCalculator, new PauseMessage());

        FileNode node = createMockFileNode(file.toString());

        assertNull(comparator.compare(node));
    }

    @Test
    void compare_processesAllDirectoryChildren() throws IOException {
        Path file1 = tempDir.resolve("file1.txt");
        Path file2 = tempDir.resolve("file2.txt");
        Files.writeString(file1, "content1");
        Files.writeString(file2, "content2");

        when(mockCalculator.calculate(any())).thenReturn(Optional.of("hash"));
        comparator = new HashComparator(mockReader, mockCalculator);

        FileNode fileNode1 = createMockFileNode(file1.toString());
        FileNode fileNode2 = createMockFileNode(file2.toString());
        DirNode dirNode = createMockDirNode(List.of(fileNode1, fileNode2));

        Map<String, Status> result = comparator.compare(dirNode);

        assertEquals(2, result.size());
        assertEquals(Status.NEW, result.get(file1.toString()));
        assertEquals(Status.NEW, result.get(file2.toString()));
    }

    @Test
    void compare_excludesFile_whenChecksumIsEmpty() throws IOException {
        Path file = tempDir.resolve("file.txt");
        Files.writeString(file, "content");

        when(mockCalculator.calculate(any())).thenReturn(Optional.empty());
        comparator = new HashComparator(mockReader, mockCalculator);

        FileNode node = createMockFileNode(file.toString());

        Map<String, Status> result = comparator.compare(node);

        assertFalse(result.containsKey(file.toString()));
    }

    @Test
    void visit_skipsFile_whenAlreadyProcessed() throws IOException {
        Path file = tempDir.resolve("file.txt");
        Files.writeString(file, "content");

        when(mockCalculator.calculate(any())).thenReturn(Optional.of("hash"));
        comparator = new HashComparator(mockReader, mockCalculator);

        FileNode node = createMockFileNode(file.toString());

        comparator.visit(node);
        comparator.visit(node);

        verify(mockCalculator, times(1)).calculate(any());
    }

    @Test
    void visit_skipsFile_whenFileDoesNotExist() {
        comparator = new HashComparator(mockReader, mockCalculator);
        FileNode node = createMockFileNode("/non/existent/file.txt");

        comparator.visit(node);

        verify(mockCalculator, never()).calculate(any());
    }

    @Test
    void visit_notifiesObservers_withFilePath() throws IOException {
        Path file = tempDir.resolve("file.txt");
        Files.writeString(file, "content");

        when(mockCalculator.calculate(any())).thenReturn(Optional.of("hash"));
        comparator = new HashComparator(mockReader, mockCalculator);

        Observer mockObserver = mock(Observer.class);
        comparator.addObserver(mockObserver);

        FileNode node = createMockFileNode(file.toString());
        comparator.visit(node);

        verify(mockObserver).update(eq(comparator), eq(file.toString()));
    }

    @Test
    void visit_stopsProcessingChildren_whenPausedDuringCalculation() throws IOException {
        Path file1 = tempDir.resolve("file1.txt");
        Path file2 = tempDir.resolve("file2.txt");
        Files.writeString(file1, "content1");
        Files.writeString(file2, "content2");

        when(mockCalculator.calculate(any())).thenAnswer(_ -> {
            comparator.update(mockCalculator, new PauseMessage());
            return Optional.of("hash");
        });
        comparator = new HashComparator(mockReader, mockCalculator);

        FileNode fileNode1 = createMockFileNode(file1.toString());
        FileNode fileNode2 = createMockFileNode(file2.toString());
        DirNode dirNode = createMockDirNode(List.of(fileNode1, fileNode2));

        comparator.visit(dirNode);

        verify(mockCalculator, times(1)).calculate(any());
    }

    @Test
    void update_setsPaused_whenPauseMessageFromCalculator() {
        comparator = new HashComparator(mockReader, mockCalculator);

        comparator.update(mockCalculator, new PauseMessage());

        FileNode node = createMockFileNode("/any/path");
        assertNull(comparator.compare(node));
    }

    @Test
    void update_ignoresPauseMessage_whenFromOtherSource() throws IOException {
        Path file = tempDir.resolve("file.txt");
        Files.writeString(file, "content");

        when(mockCalculator.calculate(any())).thenReturn(Optional.of("hash"));
        comparator = new HashComparator(mockReader, mockCalculator);

        comparator.update(mock(Observable.class), new PauseMessage());

        FileNode node = createMockFileNode(file.toString());
        Map<String, Status> result = comparator.compare(node);

        assertNotNull(result);
    }

    @Test
    void saveToMemento_capturesCurrentStatuses() throws IOException {
        Path file = tempDir.resolve("file.txt");
        Files.writeString(file, "content");

        when(mockCalculator.calculate(any())).thenReturn(Optional.of("hash"));
        comparator = new HashComparator(mockReader, mockCalculator);

        FileNode node = createMockFileNode(file.toString());
        comparator.compare(node);

        HashComparatorMemento memento = comparator.saveToMemento();

        assertNotNull(memento);
        assertEquals(Status.NEW, memento.statuses().get(file.toString()));
    }

    @Test
    void restoreFromMemento_restoresStatuses() throws IOException {
        comparator = new HashComparator(mockReader, mockCalculator);

        Map<String, Status> savedStatuses = new HashMap<>();
        savedStatuses.put("/file1.txt", Status.OK);
        savedStatuses.put("/file2.txt", Status.MODIFIED);
        comparator.restoreFromMemento(new HashComparatorMemento(savedStatuses));

        Path file3 = tempDir.resolve("file3.txt");
        Files.writeString(file3, "content");
        when(mockCalculator.calculate(any())).thenReturn(Optional.of("hash"));

        FileNode node = createMockFileNode(file3.toString());
        comparator.visit(node);

        HashComparatorMemento newMemento = comparator.saveToMemento();

        assertEquals(Status.OK, newMemento.statuses().get("/file1.txt"));
        assertEquals(Status.MODIFIED, newMemento.statuses().get("/file2.txt"));
        assertEquals(Status.NEW, newMemento.statuses().get(file3.toString()));
    }

    private FileNode createMockFileNode(String path) {
        FileNode node = mock(FileNode.class);
        when(node.getPath()).thenReturn(path);
        doAnswer(inv -> {
            FSVisitor v = inv.getArgument(0);
            v.visit(node);
            return null;
        }).when(node).accept(any());
        return node;
    }

    private DirNode createMockDirNode(List<FileNode> children) {
        DirNode node = mock(DirNode.class);
        when(node.getChildren()).thenReturn(new java.util.ArrayList<>(children));
        doAnswer(inv -> {
            FSVisitor v = inv.getArgument(0);
            v.visit(node);
            return null;
        }).when(node).accept(any());
        return node;
    }
}