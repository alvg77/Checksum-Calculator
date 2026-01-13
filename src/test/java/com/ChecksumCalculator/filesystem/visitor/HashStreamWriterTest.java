package com.ChecksumCalculator.filesystem.visitor;

import com.ChecksumCalculator.calculator.ChecksumCalculator;
import com.ChecksumCalculator.filesystem.node.DirNode;
import com.ChecksumCalculator.filesystem.node.FSNode;
import com.ChecksumCalculator.filesystem.node.FileNode;
import com.ChecksumCalculator.io.ChecksumData;
import com.ChecksumCalculator.io.serializer.checksum.ChecksumSerializer;
import com.ChecksumCalculator.progress.observer.Observable;
import com.ChecksumCalculator.progress.observer.Observer;
import com.ChecksumCalculator.state.PauseMessage;
import com.ChecksumCalculator.state.memento.HashStreamWriterMemento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class HashStreamWriterTest {

    @TempDir
    Path tempDir;

    private ChecksumSerializer mockSerializer;
    private ChecksumCalculator mockCalculator;
    private HashStreamWriter writer;

    @BeforeEach
    void setUp() {
        mockSerializer = mock(ChecksumSerializer.class);
        mockCalculator = mock(ChecksumCalculator.class);
        writer = new HashStreamWriter(mockSerializer, mockCalculator);
    }

    @Test
    void constructor_registersAsObserverOfCalculator() {
        verify(mockCalculator).addObserver(writer);
    }

    @Test
    void write_closesSerializer_afterProcessing() throws IOException {
        Path file = tempDir.resolve("file.txt");
        Files.writeString(file, "content");
        when(mockCalculator.calculate(any())).thenReturn(Optional.of("hash"));

        FileNode node = createMockFileNode(file.toString());

        writer.write(node);

        verify(mockSerializer).close();
    }

    @Test
    void write_closesSerializer_whenPaused() {
        writer.update(mockCalculator, new PauseMessage());
        FileNode node = createMockFileNode("/any/path");

        writer.write(node);

        verify(mockSerializer).close();
    }

    @Test
    void visit_writesChecksumData_forValidFile() throws IOException {
        Path file = tempDir.resolve("file.txt");
        Files.writeString(file, "test content");
        when(mockCalculator.calculate(any())).thenReturn(Optional.of("abc123"));

        FileNode node = createMockFileNode(file.toString());

        writer.visit(node);

        ArgumentCaptor<ChecksumData> captor = ArgumentCaptor.forClass(ChecksumData.class);
        verify(mockSerializer).write(captor.capture());

        ChecksumData data = captor.getValue();
        assertEquals("abc123", data.hash());
        assertEquals(file.toString(), data.path());
        assertEquals(Files.size(file), data.size());
    }

    @Test
    void visit_skipsFile_whenFileDoesNotExist() {
        FileNode node = createMockFileNode("/non/existent/file.txt");

        writer.visit(node);

        verify(mockCalculator, never()).calculate(any());
        verify(mockSerializer, never()).write(any());
    }

    @Test
    void visit_skipsFile_whenFileNotReadable() throws IOException {
        Path file = tempDir.resolve("unreadable.txt");
        Files.writeString(file, "content");
        file.toFile().setReadable(false);

        try {
            FileNode node = createMockFileNode(file.toString());

            writer.visit(node);

            verify(mockCalculator, never()).calculate(any());
            verify(mockSerializer, never()).write(any());
        } finally {
            file.toFile().setReadable(true);
        }
    }

    @Test
    void visit_skipsFile_whenAlreadyProcessed() throws IOException {
        Path file = tempDir.resolve("file.txt");
        Files.writeString(file, "content");
        when(mockCalculator.calculate(any())).thenReturn(Optional.of("hash"));

        FileNode node = createMockFileNode(file.toString());

        writer.visit(node);
        writer.visit(node);

        verify(mockCalculator, times(1)).calculate(any());
        verify(mockSerializer, times(1)).write(any());
    }

    @Test
    void visit_addsFileToProcessedSet_afterSuccessfulWrite() throws IOException {
        Path file = tempDir.resolve("file.txt");
        Files.writeString(file, "content");
        when(mockCalculator.calculate(any())).thenReturn(Optional.of("hash"));

        FileNode node = createMockFileNode(file.toString());

        writer.visit(node);

        HashStreamWriterMemento memento = writer.saveToMemento();
        assertTrue(memento.processedFiles().contains(file.toString()));
    }

    @Test
    void visit_notifiesObservers_withFilePath() throws IOException {
        Path file = tempDir.resolve("file.txt");
        Files.writeString(file, "content");
        when(mockCalculator.calculate(any())).thenReturn(Optional.of("hash"));

        Observer mockObserver = mock(Observer.class);
        writer.addObserver(mockObserver);

        FileNode node = createMockFileNode(file.toString());
        writer.visit(node);

        verify(mockObserver).update(eq(writer), eq(file.toString()));
    }

    @Test
    void visit_skipsWrite_whenChecksumIsEmpty() throws IOException {
        Path file = tempDir.resolve("file.txt");
        Files.writeString(file, "content");
        when(mockCalculator.calculate(any())).thenReturn(Optional.empty());

        FileNode node = createMockFileNode(file.toString());

        writer.visit(node);

        verify(mockSerializer, never()).write(any());
    }

    @Test
    void visit_skipsWrite_whenPausedDuringCalculation() throws IOException {
        Path file = tempDir.resolve("file.txt");
        Files.writeString(file, "content");
        when(mockCalculator.calculate(any())).thenAnswer(_ -> {
            writer.update(mockCalculator, new PauseMessage());
            return Optional.of("hash");
        });

        FileNode node = createMockFileNode(file.toString());

        writer.visit(node);

        verify(mockSerializer, never()).write(any());
    }

    @Test
    void visit_processesAllDirectoryChildren() throws IOException {
        Path file1 = tempDir.resolve("file1.txt");
        Path file2 = tempDir.resolve("file2.txt");
        Files.writeString(file1, "content1");
        Files.writeString(file2, "content2");
        when(mockCalculator.calculate(any())).thenReturn(Optional.of("hash"));

        FileNode fileNode1 = createMockFileNode(file1.toString());
        FileNode fileNode2 = createMockFileNode(file2.toString());
        DirNode dirNode = createMockDirNode(List.of(fileNode1, fileNode2));

        writer.visit(dirNode);

        verify(mockSerializer, times(2)).write(any(ChecksumData.class));
    }

    @Test
    void visit_stopsProcessingChildren_whenPaused() throws IOException {
        Path file1 = tempDir.resolve("file1.txt");
        Path file2 = tempDir.resolve("file2.txt");
        Files.writeString(file1, "content1");
        Files.writeString(file2, "content2");

        when(mockCalculator.calculate(any())).thenAnswer(_ -> {
            writer.update(mockCalculator, new PauseMessage());
            return Optional.of("hash");
        });

        FileNode fileNode1 = createMockFileNode(file1.toString());
        FileNode fileNode2 = createMockFileNode(file2.toString());
        DirNode dirNode = createMockDirNode(List.of(fileNode1, fileNode2));

        writer.visit(dirNode);

        verify(mockCalculator, times(1)).calculate(any());
    }

    @Test
    void visit_handlesNestedDirectories() throws IOException {
        Path file = tempDir.resolve("file.txt");
        Files.writeString(file, "content");
        when(mockCalculator.calculate(any())).thenReturn(Optional.of("hash"));

        FileNode fileNode = createMockFileNode(file.toString());
        DirNode innerDir = createMockDirNode(List.of(fileNode));
        DirNode outerDir = createMockDirNode(List.of(innerDir));

        writer.visit(outerDir);

        verify(mockSerializer).write(any(ChecksumData.class));
    }

    @Test
    void update_setsPaused_whenPauseMessageFromCalculator() throws IOException {
        Path file = tempDir.resolve("file.txt");
        Files.writeString(file, "content");
        when(mockCalculator.calculate(any())).thenReturn(Optional.of("hash"));

        writer.update(mockCalculator, new PauseMessage());

        FileNode node = createMockFileNode(file.toString());
        writer.visit(node);

        verify(mockCalculator, never()).calculate(any());
    }

    @Test
    void update_ignoresPauseMessage_whenFromOtherSource() throws IOException {
        Path file = tempDir.resolve("file.txt");
        Files.writeString(file, "content");
        when(mockCalculator.calculate(any())).thenReturn(Optional.of("hash"));

        writer.update(mock(Observable.class), new PauseMessage());

        FileNode node = createMockFileNode(file.toString());
        writer.visit(node);

        verify(mockSerializer).write(any());
    }

    @Test
    void saveToMemento_capturesProcessedFiles() throws IOException {
        Path file1 = tempDir.resolve("file1.txt");
        Path file2 = tempDir.resolve("file2.txt");
        Files.writeString(file1, "content1");
        Files.writeString(file2, "content2");
        when(mockCalculator.calculate(any())).thenReturn(Optional.of("hash"));

        writer.visit(createMockFileNode(file1.toString()));
        writer.visit(createMockFileNode(file2.toString()));

        HashStreamWriterMemento memento = writer.saveToMemento();

        assertEquals(2, memento.processedFiles().size());
        assertTrue(memento.processedFiles().contains(file1.toString()));
        assertTrue(memento.processedFiles().contains(file2.toString()));
    }

    @Test
    void restoreFromMemento_restoresProcessedFiles() {
        Set<String> savedFiles = new HashSet<>();
        savedFiles.add("/file1.txt");
        savedFiles.add("/file2.txt");

        writer.restoreFromMemento(new HashStreamWriterMemento(savedFiles));

        HashStreamWriterMemento newMemento = writer.saveToMemento();
        assertEquals(2, newMemento.processedFiles().size());
        assertTrue(newMemento.processedFiles().contains("/file1.txt"));
        assertTrue(newMemento.processedFiles().contains("/file2.txt"));
    }

    @Test
    void restoreFromMemento_skipsRestoredFilesOnRevisit() throws IOException {
        Path file = tempDir.resolve("file.txt");
        Files.writeString(file, "content");

        Set<String> savedFiles = new HashSet<>();
        savedFiles.add(file.toString());
        writer.restoreFromMemento(new HashStreamWriterMemento(savedFiles));

        FileNode node = createMockFileNode(file.toString());
        writer.visit(node);

        verify(mockCalculator, never()).calculate(any());
        verify(mockSerializer, never()).write(any());
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

    private DirNode createMockDirNode(List<FSNode> children) {
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