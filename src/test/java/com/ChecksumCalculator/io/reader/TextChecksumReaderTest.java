package com.ChecksumCalculator.io.reader;

import com.ChecksumCalculator.exception.FileSystemException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TextChecksumReaderTest {
    @TempDir
    Path tempDir;

    private Path checksumFile;

    @BeforeEach
    void setUp() {
        checksumFile = tempDir.resolve("checksums.txt");
    }

    @Test
    void readHashes_returnsSingleEntry() throws IOException {
        Files.writeString(checksumFile, "abc123 /path/to/file.txt 1024\n");

        TextChecksumReader reader = new TextChecksumReader(checksumFile.toString());
        Map<String, String> hashes = reader.readHashes();

        assertEquals(1, hashes.size());
        assertEquals("abc123", hashes.get("/path/to/file.txt"));
    }

    @Test
    void readHashes_returnsMultipleEntries() throws IOException {
        String content = """
                hash1 /file1.txt 100
                hash2 /file2.txt 200
                hash3 /file3.txt 300
                """;
        Files.writeString(checksumFile, content);

        TextChecksumReader reader = new TextChecksumReader(checksumFile.toString());
        Map<String, String> hashes = reader.readHashes();

        assertEquals(3, hashes.size());
        assertEquals("hash1", hashes.get("/file1.txt"));
        assertEquals("hash2", hashes.get("/file2.txt"));
        assertEquals("hash3", hashes.get("/file3.txt"));
    }

    @Test
    void readHashes_handlesPathsWithSpaces() throws IOException {
        Files.writeString(checksumFile, "abc123 /path/with spaces/file name.txt 1024\n");

        TextChecksumReader reader = new TextChecksumReader(checksumFile.toString());
        Map<String, String> hashes = reader.readHashes();

        assertEquals("abc123", hashes.get("/path/with spaces/file name.txt"));
    }

    @Test
    void readHashes_handlesPathsWithMultipleSpaces() throws IOException {
        Files.writeString(checksumFile, "hash /a b c/d e f.txt 500\n");

        TextChecksumReader reader = new TextChecksumReader(checksumFile.toString());
        Map<String, String> hashes = reader.readHashes();

        assertEquals("hash", hashes.get("/a b c/d e f.txt"));
    }

    @Test
    void readHashes_skipsBlankLines() throws IOException {
        String content = """
                hash1 /file1.txt 100
                
                hash2 /file2.txt 200
                
                """;
        Files.writeString(checksumFile, content);

        TextChecksumReader reader = new TextChecksumReader(checksumFile.toString());
        Map<String, String> hashes = reader.readHashes();

        assertEquals(2, hashes.size());
    }

    @Test
    void readHashes_skipsMalformedLines_withoutFirstSpace() throws IOException {
        String content = """
                hash1 /file1.txt 100
                malformedlinewithoutspace
                hash2 /file2.txt 200
                """;
        Files.writeString(checksumFile, content);

        TextChecksumReader reader = new TextChecksumReader(checksumFile.toString());
        Map<String, String> hashes = reader.readHashes();

        assertEquals(2, hashes.size());
    }

    @Test
    void readHashes_skipsMalformedLines_withoutSize() throws IOException {
        String content = """
                hash1 /file1.txt 100
                hash2 /file2.txt
                hash3 /file3.txt 300
                """;
        Files.writeString(checksumFile, content);

        TextChecksumReader reader = new TextChecksumReader(checksumFile.toString());
        Map<String, String> hashes = reader.readHashes();

        assertEquals(2, hashes.size());
        assertNull(hashes.get("/file2.txt"));
    }

    @Test
    void readHashes_returnsEmptyMap_forEmptyFile() throws IOException {
        Files.writeString(checksumFile, "");

        TextChecksumReader reader = new TextChecksumReader(checksumFile.toString());
        Map<String, String> hashes = reader.readHashes();

        assertTrue(hashes.isEmpty());
    }

    @Test
    void readHashes_handlesSHA256Hash() throws IOException {
        String sha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        Files.writeString(checksumFile, sha256 + " /file.txt 100\n");

        TextChecksumReader reader = new TextChecksumReader(checksumFile.toString());
        Map<String, String> hashes = reader.readHashes();

        assertEquals(sha256, hashes.get("/file.txt"));
    }

    @Test
    void readHashes_handlesWindowsPaths() throws IOException {
        Files.writeString(checksumFile, "hash C:\\Users\\test\\file.txt 100\n");

        TextChecksumReader reader = new TextChecksumReader(checksumFile.toString());
        Map<String, String> hashes = reader.readHashes();

        assertEquals("hash", hashes.get("C:\\Users\\test\\file.txt"));
    }

    @Test
    void readHashes_overwritesDuplicatePaths_withLastValue() throws IOException {
        String content = """
                hash1 /file.txt 100
                hash2 /file.txt 200
                """;
        Files.writeString(checksumFile, content);

        TextChecksumReader reader = new TextChecksumReader(checksumFile.toString());
        Map<String, String> hashes = reader.readHashes();

        assertEquals(1, hashes.size());
        assertEquals("hash2", hashes.get("/file.txt"));
    }

    @Test
    void readHashes_handlesLineWithoutTrailingNewline() throws IOException {
        Files.writeString(checksumFile, "hash /file.txt 100");

        TextChecksumReader reader = new TextChecksumReader(checksumFile.toString());
        Map<String, String> hashes = reader.readHashes();

        assertEquals("hash", hashes.get("/file.txt"));
    }

    @Test
    void readHashes_throwsFileSystemException_forNonExistentFile() {
        TextChecksumReader reader = new TextChecksumReader("/non/existent/file.txt");

        FileSystemException exception = assertThrows(
                FileSystemException.class,
                reader::readHashes
        );

        assertTrue(exception.getMessage().contains("/non/existent/file.txt"));
    }

    @Test
    void readHashes_throwsFileSystemException_forDirectory() {
        TextChecksumReader reader = new TextChecksumReader(tempDir.toString());

        assertThrows(FileSystemException.class, reader::readHashes);
    }

    @Test
    void readHashes_handlesLargeNumberOfEntries() throws IOException {
        StringBuilder content = new StringBuilder();
        int count = 10000;
        for (int i = 0; i < count; i++) {
            content.append("hash").append(i).append(" /file").append(i).append(".txt ").append(i).append("\n");
        }
        Files.writeString(checksumFile, content.toString());

        TextChecksumReader reader = new TextChecksumReader(checksumFile.toString());
        Map<String, String> hashes = reader.readHashes();

        assertEquals(count, hashes.size());
    }
}