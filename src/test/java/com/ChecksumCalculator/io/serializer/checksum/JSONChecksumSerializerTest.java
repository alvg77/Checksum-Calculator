package com.ChecksumCalculator.io.serializer.checksum;

import com.ChecksumCalculator.io.ChecksumData;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JSONChecksumSerializerTest {

    private ByteArrayOutputStream outputStream;
    private JSONChecksumSerializer serializer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        outputStream = new ByteArrayOutputStream();
        serializer = new JSONChecksumSerializer(outputStream);
        objectMapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() {
        try {
            serializer.close();
        } catch (Exception ignored) {
        }
    }

    @Test
    void write_producesSingleEntryJsonArray() throws IOException {
        serializer.write(new ChecksumData("abc123", "/path/to/file.txt", 1024L));
        serializer.close();

        List<ChecksumData> result = parseOutput();

        assertEquals(1, result.size());
        assertEquals("abc123", result.getFirst().hash());
        assertEquals("/path/to/file.txt", result.getFirst().path());
        assertEquals(1024L, result.getFirst().size());
    }

    @Test
    void write_producesMultipleEntryJsonArray() throws IOException {
        serializer.write(new ChecksumData("hash1", "/file1.txt", 100L));
        serializer.write(new ChecksumData("hash2", "/file2.txt", 200L));
        serializer.write(new ChecksumData("hash3", "/file3.txt", 300L));
        serializer.close();

        List<ChecksumData> result = parseOutput();

        assertEquals(3, result.size());
        assertEquals("hash1", result.get(0).hash());
        assertEquals("hash2", result.get(1).hash());
        assertEquals("hash3", result.get(2).hash());
    }

    @Test
    void write_producesPrettyPrintedJson() {
        serializer.write(new ChecksumData("hash", "/file.txt", 100L));
        serializer.close();

        String json = outputStream.toString();
        assertTrue(json.contains("\n") || json.contains(System.lineSeparator()));
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void write_escapesSpecialCharactersInPaths() throws IOException {
        serializer.write(new ChecksumData("hash", "/path/with\"quotes\".txt", 100L));
        serializer.close();

        String json = outputStream.toString();
        assertTrue(json.contains("\\\""));

        List<ChecksumData> result = parseOutput();
        assertEquals("/path/with\"quotes\".txt", result.getFirst().path());
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void write_handlesWindowsPathsWithBackslashes() throws IOException {
        serializer.write(new ChecksumData("hash", "C:\\Users\\test\\file.txt", 100L));
        serializer.close();

        List<ChecksumData> result = parseOutput();
        assertEquals("C:\\Users\\test\\file.txt", result.getFirst().path());
    }

    @Test
    void write_handlesLargeFileSize() throws IOException {
        long largeSize = 10_000_000_000L;
        serializer.write(new ChecksumData("hash", "/large.bin", largeSize));
        serializer.close();

        List<ChecksumData> result = parseOutput();
        assertEquals(largeSize, result.getFirst().size());
    }

    @Test
    void write_preservesEntryOrder() throws IOException {
        for (int i = 0; i < 10; i++) {
            serializer.write(new ChecksumData("hash" + i, "/file" + i + ".txt", i));
        }
        serializer.close();

        List<ChecksumData> result = parseOutput();

        for (int i = 0; i < 10; i++) {
            assertEquals("hash" + i, result.get(i).hash());
            assertEquals("/file" + i + ".txt", result.get(i).path());
            assertEquals(i, result.get(i).size());
        }
    }

    @Test
    void close_producesValidJsonArray() {
        serializer.write(new ChecksumData("hash", "/file.txt", 100L));
        serializer.close();

        String json = outputStream.toString().trim();
        assertTrue(json.startsWith("["));
        assertTrue(json.endsWith("]"));
        assertDoesNotThrow(() -> objectMapper.readTree(json));
    }

    @Test
    void close_handlesNoWrites() {
        assertDoesNotThrow(() -> serializer.close());

        String json = outputStream.toString();
        assertTrue(json.isEmpty() || json.equals("[ ]") || json.equals("[]"));
    }

    @Test
    void close_outputContainsCorrectFields() throws IOException {
        serializer.write(new ChecksumData("testhash", "/test/path.txt", 999L));
        serializer.close();

        var jsonNode = objectMapper.readTree(outputStream.toString());
        var firstElement = jsonNode.get(0);

        assertTrue(firstElement.has("hash"));
        assertTrue(firstElement.has("path"));
        assertTrue(firstElement.has("size"));

        assertEquals("testhash", firstElement.get("hash").asText());
        assertEquals("/test/path.txt", firstElement.get("path").asText());
        assertEquals(999L, firstElement.get("size").asLong());
    }

    @Test
    void tryWithResources_closesSerializerAutomatically() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (JSONChecksumSerializer autoSerializer = new JSONChecksumSerializer(baos)) {
            autoSerializer.write(new ChecksumData("hash", "/auto.txt", 50L));
        }

        List<ChecksumData> result = objectMapper.readValue(baos.toString(), new TypeReference<>() {});
        assertEquals(1, result.size());
        assertEquals("hash", result.getFirst().hash());
    }

    @Test
    void write_handlesLargeNumberOfEntries() throws IOException {
        int count = 1000;

        for (int i = 0; i < count; i++) {
            serializer.write(new ChecksumData("hash" + i, "/file" + i + ".txt", i));
        }
        serializer.close();

        List<ChecksumData> result = parseOutput();
        assertEquals(count, result.size());
    }

    private List<ChecksumData> parseOutput() throws IOException {
        return objectMapper.readValue(outputStream.toString(), new TypeReference<>() {});
    }
}