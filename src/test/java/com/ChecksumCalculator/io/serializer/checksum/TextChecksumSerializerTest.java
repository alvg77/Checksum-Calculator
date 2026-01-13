package com.ChecksumCalculator.io.serializer.checksum;

import com.ChecksumCalculator.io.ChecksumData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class TextChecksumSerializerTest {

    private ByteArrayOutputStream outputStream;
    private TextChecksumSerializer serializer;

    @BeforeEach
    void setUp() {
        outputStream = new ByteArrayOutputStream();
        serializer = new TextChecksumSerializer(outputStream);
    }

    @AfterEach
    void tearDown() {
        serializer.close();
    }

    @Test
    void write_producesCorrectFormat() {
        serializer.write(new ChecksumData("abc123", "/path/to/file.txt", 1024L));
        serializer.close();

        String output = outputStream.toString().trim();
        assertEquals("abc123 /path/to/file.txt 1024", output);
    }

    @Test
    void write_producesMultipleEntriesOnSeparateLines() {
        serializer.write(new ChecksumData("hash1", "/file1.txt", 100L));
        serializer.write(new ChecksumData("hash2", "/file2.txt", 200L));
        serializer.write(new ChecksumData("hash3", "/file3.txt", 300L));
        serializer.close();

        String[] lines = outputStream.toString().trim().split(System.lineSeparator());
        assertEquals(3, lines.length);
        assertEquals("hash1 /file1.txt 100", lines[0]);
        assertEquals("hash2 /file2.txt 200", lines[1]);
        assertEquals("hash3 /file3.txt 300", lines[2]);
    }

    @Test
    void write_handlesLargeFileSize() {
        long largeSize = 10_000_000_000L;
        serializer.write(new ChecksumData("hash", "/large.bin", largeSize));
        serializer.close();

        String output = outputStream.toString().trim();
        assertEquals("hash /large.bin 10000000000", output);
    }

    @Test
    void write_handlesPathsWithSpaces() {
        serializer.write(new ChecksumData("hash", "/path/with spaces/file name.txt", 512L));
        serializer.close();

        String output = outputStream.toString().trim();
        assertEquals("hash /path/with spaces/file name.txt 512", output);
    }

    @Test
    void write_handlesSHA256Hash() {
        String sha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        serializer.write(new ChecksumData(sha256, "/file.txt", 100L));
        serializer.close();

        String output = outputStream.toString().trim();
        assertTrue(output.startsWith(sha256));
    }

    @Test
    void close_flushesContent() {
        serializer.write(new ChecksumData("hash", "/test.txt", 100L));
        serializer.close();

        String output = outputStream.toString();
        assertFalse(output.isEmpty());
    }

    @Test
    void close_handlesNoWrites() {
        assertDoesNotThrow(() -> serializer.close());
        assertEquals("", outputStream.toString());
    }

    @Test
    void tryWithResources_closesSerializerAutomatically() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (TextChecksumSerializer autoSerializer = new TextChecksumSerializer(baos)) {
            autoSerializer.write(new ChecksumData("hash", "/auto.txt", 50L));
        }

        String output = baos.toString().trim();
        assertEquals("hash /auto.txt 50", output);
    }

    @Test
    void write_handlesLargeNumberOfEntries() {
        int count = 10000;

        for (int i = 0; i < count; i++) {
            serializer.write(new ChecksumData("hash" + i, "/file" + i + ".txt", (long) i));
        }
        serializer.close();

        String[] lines = outputStream.toString().trim().split(System.lineSeparator());
        assertEquals(count, lines.length);
    }
}