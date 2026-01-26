package com.ChecksumCalculator.io.serializer;

import com.ChecksumCalculator.filesystem.visitor.Status;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ComparisonResultSerializerTest {
    private ByteArrayOutputStream outputStream;
    private ComparisonResultSerializer serializer;

    @BeforeEach
    void setUp() {
        outputStream = new ByteArrayOutputStream();
        serializer = new ComparisonResultSerializer(outputStream);
    }

    @AfterEach
    void tearDown() {
        serializer.close();
    }

    @Test
    void write_producesSingleEntry() {
        Map<String, Status> results = new LinkedHashMap<>();
        results.put("/path/to/file.txt", Status.OK);

        serializer.write(results);
        serializer.close();

        String output = outputStream.toString();
        assertTrue(output.contains("/path/to/file.txt: OK"));
    }

    @Test
    void write_producesMultipleEntries() {
        Map<String, Status> results = new LinkedHashMap<>();
        results.put("/file1.txt", Status.OK);
        results.put("/file2.txt", Status.MODIFIED);
        results.put("/file3.txt", Status.NEW);

        serializer.write(results);
        serializer.close();

        String output = outputStream.toString();
        assertTrue(output.contains("/file1.txt: OK"));
        assertTrue(output.contains("/file2.txt: MODIFIED"));
        assertTrue(output.contains("/file3.txt: NEW"));
    }

    @Test
    void write_handlesAllStatusTypes() {
        Map<String, Status> results = new LinkedHashMap<>();
        for (Status status : Status.values()) {
            results.put("/file_" + status.name() + ".txt", status);
        }

        serializer.write(results);
        serializer.close();

        String output = outputStream.toString();
        for (Status status : Status.values()) {
            assertTrue(output.contains(status.toString()));
        }
    }

    @Test
    void write_handlesPathsWithSpaces() {
        Map<String, Status> results = new LinkedHashMap<>();
        results.put("/path/with spaces/file.txt", Status.OK);

        serializer.write(results);
        serializer.close();

        String output = outputStream.toString();
        assertTrue(output.contains("/path/with spaces/file.txt: OK"));
    }

    @Test
    void write_producesEntriesOnSeparateLines() {
        Map<String, Status> results = new LinkedHashMap<>();
        results.put("/file1.txt", Status.OK);
        results.put("/file2.txt", Status.MODIFIED);

        serializer.write(results);
        serializer.close();

        String output = outputStream.toString();
        String[] lines = output.split(System.lineSeparator());

        long entryLines = java.util.Arrays.stream(lines)
                .filter(line -> line.contains(": "))
                .filter(line -> !line.contains("======"))
                .count();

        assertEquals(2, entryLines);
    }

    @Test
    void close_flushesContent() {
        Map<String, Status> results = new LinkedHashMap<>();
        results.put("/test.txt", Status.OK);

        serializer.write(results);
        serializer.close();

        String output = outputStream.toString();
        assertFalse(output.isEmpty());
    }

    @Test
    void tryWithResources_closesSerializerAutomatically() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ComparisonResultSerializer autoSerializer = new ComparisonResultSerializer(baos)) {
            Map<String, Status> results = new LinkedHashMap<>();
            results.put("/auto.txt", Status.OK);
            autoSerializer.write(results);
        }

        String output = baos.toString();
        assertTrue(output.contains("/auto.txt: OK"));
    }

    @Test
    void write_handlesLargeNumberOfEntries() {
        Map<String, Status> results = new LinkedHashMap<>();
        int count = 10000;

        for (int i = 0; i < count; i++) {
            results.put("/file" + i + ".txt", Status.OK);
        }

        serializer.write(results);
        serializer.close();

        String output = outputStream.toString();
        assertTrue(output.contains("/file0.txt: OK"));
        assertTrue(output.contains("/file" + (count - 1) + ".txt: OK"));
    }
}