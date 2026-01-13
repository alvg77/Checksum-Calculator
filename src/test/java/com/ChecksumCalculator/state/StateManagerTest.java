package com.ChecksumCalculator.state;

import com.ChecksumCalculator.exception.FileSystemException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StateManagerTest {

    private StateManager stateManager;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        stateManager = new StateManager();
    }

    @Test
    void save_createsFile_whenStringProvided() {
        File file = tempDir.resolve("state.dat").toFile();
        String data = "test data";

        assertDoesNotThrow(() -> stateManager.save(data, file));
        assertTrue(file.exists());
        assertTrue(file.length() > 0);
    }

    @Test
    void save_createsFile_whenIntegerProvided() {
        File file = tempDir.resolve("integer.dat").toFile();

        stateManager.save(42, file);

        assertTrue(file.exists());
    }

    @Test
    void save_createsFile_whenLongProvided() {
        File file = tempDir.resolve("long.dat").toFile();

        stateManager.save(123456789L, file);

        assertTrue(file.exists());
    }

    @Test
    void save_createsFile_whenArrayListProvided() {
        File file = tempDir.resolve("list.dat").toFile();
        ArrayList<String> list = new ArrayList<>(List.of("a", "b", "c"));

        stateManager.save(list, file);

        assertTrue(file.exists());
    }

    @Test
    void save_createsFile_whenHashMapProvided() {
        File file = tempDir.resolve("map.dat").toFile();
        HashMap<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        map.put("two", 2);

        stateManager.save(map, file);

        assertTrue(file.exists());
    }

    @Test
    void save_createsFile_whenCustomSerializableObjectProvided() {
        File file = tempDir.resolve("custom.dat").toFile();
        TestMemento memento = new TestMemento("test", 100L);

        stateManager.save(memento, file);

        assertTrue(file.exists());
    }

    @Test
    void save_overwritesContent_whenFileAlreadyExists() throws IOException {
        File file = tempDir.resolve("overwrite.dat").toFile();

        stateManager.save("first", file);
        long firstSize = file.length();

        stateManager.save("second value that is longer", file);
        long secondSize = file.length();

        assertNotEquals(firstSize, secondSize);
    }

    @Test
    void save_createsFile_whenDirectoryIsNested() throws IOException {
        Path nested = tempDir.resolve("sub/dir");
        Files.createDirectories(nested);
        File file = nested.resolve("state.dat").toFile();

        stateManager.save("data", file);

        assertTrue(file.exists());
    }

    @Test
    void save_throwsFileSystemException_whenDirectoryDoesNotExist() {
        File file = new File("/non/existent/directory/state.dat");

        FileSystemException exception = assertThrows(
                FileSystemException.class,
                () -> stateManager.save("data", file)
        );

        assertTrue(exception.getMessage().contains(file.getAbsolutePath()));
    }

    @Test
    void save_throwsFileSystemException_whenLocationIsReadOnly() throws IOException {
        File file = tempDir.resolve("readonly.dat").toFile();
        file.createNewFile();
        file.setWritable(false);

        try {
            FileSystemException exception = assertThrows(
                    FileSystemException.class,
                    () -> stateManager.save("data", file)
            );
            assertTrue(exception.getMessage().contains("Could not save state"));
        } finally {
            file.setWritable(true);
        }
    }

    @Test
    void save_handlesNull_byCreatingFile() {
        File file = tempDir.resolve("null.dat").toFile();

        assertDoesNotThrow(() -> stateManager.save(null, file));
        assertTrue(file.exists());
    }

    @Test
    void save_preservesExceptionCause_whenIoFailureOccurs() {
        File file = new File("/invalid/path/state.dat");

        FileSystemException exception = assertThrows(
                FileSystemException.class,
                () -> stateManager.save("data", file)
        );

        assertNotNull(exception.getCause());
        assertTrue(exception.getCause() instanceof IOException);
    }

    @Test
    void load_returnsString_whenStringWasSaved() {
        File file = tempDir.resolve("string.dat").toFile();
        String original = "test data";
        stateManager.save(original, file);

        Serializable loaded = stateManager.load(file);

        assertEquals(original, loaded);
    }

    @Test
    void load_returnsInteger_whenIntegerWasSaved() {
        File file = tempDir.resolve("integer.dat").toFile();
        stateManager.save(42, file);

        Serializable loaded = stateManager.load(file);

        assertEquals(42, loaded);
    }

    @Test
    void load_returnsLong_whenLongWasSaved() {
        File file = tempDir.resolve("long.dat").toFile();
        Long original = 9876543210L;
        stateManager.save(original, file);

        Serializable loaded = stateManager.load(file);

        assertEquals(original, loaded);
    }

    @Test
    void load_returnsArrayList_whenListWasSaved() {
        File file = tempDir.resolve("list.dat").toFile();
        ArrayList<String> original = new ArrayList<>(List.of("x", "y", "z"));
        stateManager.save(original, file);

        Serializable loaded = stateManager.load(file);

        assertEquals(original, loaded);
    }

    @Test
    void load_returnsHashMap_whenMapWasSaved() {
        File file = tempDir.resolve("map.dat").toFile();
        HashMap<String, Integer> original = new HashMap<>();
        original.put("key1", 100);
        original.put("key2", 200);
        stateManager.save(original, file);

        Serializable loaded = stateManager.load(file);

        assertEquals(original, loaded);
    }

    @Test
    void load_returnsCustomObject_whenObjectWasSaved() {
        File file = tempDir.resolve("custom.dat").toFile();
        TestMemento original = new TestMemento("name", 500L);
        stateManager.save(original, file);

        Serializable loaded = stateManager.load(file);

        assertTrue(loaded instanceof TestMemento);
        TestMemento loadedMemento = (TestMemento) loaded;
        assertEquals("name", loadedMemento.name());
        assertEquals(500L, loadedMemento.value());
    }

    @Test
    void load_returnsNull_whenNullWasSaved() {
        File file = tempDir.resolve("null.dat").toFile();
        stateManager.save(null, file);

        Serializable loaded = stateManager.load(file);

        assertNull(loaded);
    }

    @Test
    void load_throwsFileSystemException_whenFileDoesNotExist() {
        File file = new File("/non/existent/file.dat");

        FileSystemException exception = assertThrows(
                FileSystemException.class,
                () -> stateManager.load(file)
        );

        assertTrue(exception.getMessage().contains("Cannot load memento"));
        assertTrue(exception.getMessage().contains(file.getPath()));
    }

    @Test
    void load_throwsFileSystemException_whenFileIsCorrupted() throws IOException {
        File file = tempDir.resolve("corrupted.dat").toFile();
        Files.writeString(file.toPath(), "not valid serialized data");

        FileSystemException exception = assertThrows(
                FileSystemException.class,
                () -> stateManager.load(file)
        );

        assertTrue(exception.getMessage().contains("Cannot load"));
    }

    @Test
    void load_throwsFileSystemException_whenFileIsEmpty() throws IOException {
        File file = tempDir.resolve("empty.dat").toFile();
        file.createNewFile();

        assertThrows(FileSystemException.class, () -> stateManager.load(file));
    }

    @Test
    void load_throwsFileSystemException_whenFileIsTruncated() throws IOException {
        File file = tempDir.resolve("truncated.dat").toFile();
        stateManager.save("some data", file);

        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.setLength(file.length() / 2);
        }

        assertThrows(FileSystemException.class, () -> stateManager.load(file));
    }

    @Test
    void load_throwsFileSystemException_whenPathIsDirectory() {
        File dir = tempDir.toFile();

        assertThrows(FileSystemException.class, () -> stateManager.load(dir));
    }

    @Test
    void load_preservesExceptionCause_whenIoFailureOccurs() {
        File file = new File("/non/existent/file.dat");

        FileSystemException exception = assertThrows(
                FileSystemException.class,
                () -> stateManager.load(file)
        );

        assertNotNull(exception.getCause());
        assertTrue(exception.getCause() instanceof IOException);
    }

    @Test
    void saveAndLoad_preservesSpecialCharacters() {
        File file = tempDir.resolve("roundtrip.dat").toFile();
        String original = "Hello, World! Special chars: äöü 日本語";

        stateManager.save(original, file);
        Serializable loaded = stateManager.load(file);

        assertEquals(original, loaded);
    }

    @Test
    void saveAndLoad_preservesComplexNestedStructure() {
        File file = tempDir.resolve("complex.dat").toFile();

        HashMap<String, ArrayList<Integer>> original = new HashMap<>();
        original.put("primes", new ArrayList<>(List.of(2, 3, 5, 7, 11)));
        original.put("fibonacci", new ArrayList<>(List.of(1, 1, 2, 3, 5, 8)));

        stateManager.save(original, file);

        @SuppressWarnings("unchecked")
        Map<String, ArrayList<Integer>> loaded =
                (Map<String, ArrayList<Integer>>) stateManager.load(file);

        assertEquals(original, loaded);
    }

    @Test
    void saveAndLoad_preservesCustomMemento_throughRoundTrip() {
        File file = tempDir.resolve("memento.dat").toFile();
        TestMemento original = new TestMemento("progress", 12345L);

        stateManager.save(original, file);
        TestMemento loaded = (TestMemento) stateManager.load(file);

        assertEquals(original.name(), loaded.name());
        assertEquals(original.value(), loaded.value());
    }

    @Test
    void saveAndLoad_handlesMultipleCycles() {
        File file = tempDir.resolve("cycles.dat").toFile();

        for (int i = 0; i < 10; i++) {
            String data = "iteration " + i;
            stateManager.save(data, file);
            Serializable loaded = stateManager.load(file);
            assertEquals(data, loaded);
        }
    }

    @Test
    void saveAndLoad_preservesLargeData() {
        File file = tempDir.resolve("large.dat").toFile();
        ArrayList<String> original = new ArrayList<>();

        for (int i = 0; i < 10000; i++) {
            original.add("item-" + i + "-" + "x".repeat(100));
        }

        stateManager.save(original, file);

        @SuppressWarnings("unchecked")
        ArrayList<String> loaded = (ArrayList<String>) stateManager.load(file);

        assertEquals(original.size(), loaded.size());
        assertEquals(original, loaded);
    }

    @Test
    void saveAndLoad_handlesFileWithSpecialCharactersInName() {
        File file = tempDir.resolve("state-2024_01_01.dat").toFile();

        stateManager.save("data", file);
        Serializable loaded = stateManager.load(file);

        assertEquals("data", loaded);
    }

    @Test
    void saveAndLoad_handlesVeryLongFilePaths() throws IOException {
        Path nested = tempDir;
        for (int i = 0; i < 10; i++) {
            nested = nested.resolve("dir" + i);
        }
        Files.createDirectories(nested);
        File file = nested.resolve("state.dat").toFile();

        stateManager.save("data", file);
        Serializable loaded = stateManager.load(file);

        assertEquals("data", loaded);
    }

    @Test
    void saveAndLoad_handlesEmptyStrings() {
        File file = tempDir.resolve("empty-string.dat").toFile();

        stateManager.save("", file);
        Serializable loaded = stateManager.load(file);

        assertEquals("", loaded);
    }

    @Test
    void saveAndLoad_handlesEmptyCollections() {
        File file = tempDir.resolve("empty-list.dat").toFile();
        ArrayList<String> empty = new ArrayList<>();

        stateManager.save(empty, file);

        @SuppressWarnings("unchecked")
        ArrayList<String> loaded = (ArrayList<String>) stateManager.load(file);

        assertTrue(loaded.isEmpty());
    }

    @Test
    void sequentialOperations_handleMultipleFiles_fromSameInstance() {
        File file1 = tempDir.resolve("seq1.dat").toFile();
        File file2 = tempDir.resolve("seq2.dat").toFile();

        stateManager.save("data1", file1);
        stateManager.save("data2", file2);

        assertEquals("data1", stateManager.load(file1));
        assertEquals("data2", stateManager.load(file2));
    }

    @Test
    void differentInstances_canShareState_viaFileSystem() {
        StateManager manager1 = new StateManager();
        StateManager manager2 = new StateManager();
        File file = tempDir.resolve("shared.dat").toFile();

        manager1.save("from manager1", file);
        Serializable loaded = manager2.load(file);

        assertEquals("from manager1", loaded);
    }

    record TestMemento(String name, long value) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
    }
}