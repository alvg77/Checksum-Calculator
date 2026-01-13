package com.ChecksumCalculator.io.reader;

import com.ChecksumCalculator.exception.ChecksumFileException;
import com.ChecksumCalculator.exception.FileSystemException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JSONChecksumReaderTest {

    @TempDir
    Path tempDir;

    private Path checksumFile;

    @BeforeEach
    void setUp() {
        checksumFile = tempDir.resolve("checksums.json");
    }

    @Test
    void readHashes_returnsSingleEntry() throws IOException {
        String json = """
                [
                    {"hash": "abc123", "path": "/path/to/file.txt", "size": 1024}
                ]
                """;
        Files.writeString(checksumFile, json);

        JSONChecksumReader reader = new JSONChecksumReader(checksumFile.toString());
        Map<String, String> hashes = reader.readHashes();

        assertEquals(1, hashes.size());
        assertEquals("abc123", hashes.get("/path/to/file.txt"));
    }

    @Test
    void readHashes_returnsMultipleEntries() throws IOException {
        String json = """
                [
                    {"hash": "hash1", "path": "/file1.txt", "size": 100},
                    {"hash": "hash2", "path": "/file2.txt", "size": 200},
                    {"hash": "hash3", "path": "/file3.txt", "size": 300}
                ]
                """;
        Files.writeString(checksumFile, json);

        JSONChecksumReader reader = new JSONChecksumReader(checksumFile.toString());
        Map<String, String> hashes = reader.readHashes();

        assertEquals(3, hashes.size());
        assertEquals("hash1", hashes.get("/file1.txt"));
        assertEquals("hash2", hashes.get("/file2.txt"));
        assertEquals("hash3", hashes.get("/file3.txt"));
    }

    @Test
    void readHashes_returnsEmptyMap_forEmptyArray() throws IOException {
        Files.writeString(checksumFile, "[]");

        JSONChecksumReader reader = new JSONChecksumReader(checksumFile.toString());
        Map<String, String> hashes = reader.readHashes();

        assertTrue(hashes.isEmpty());
    }

    @Test
    void readHashes_handlesPathsWithSpaces() throws IOException {
        String json = """
                [
                    {"hash": "abc123", "path": "/path/with spaces/file name.txt", "size": 1024}
                ]
                """;
        Files.writeString(checksumFile, json);

        JSONChecksumReader reader = new JSONChecksumReader(checksumFile.toString());
        Map<String, String> hashes = reader.readHashes();

        assertEquals("abc123", hashes.get("/path/with spaces/file name.txt"));
    }

    @Test
    void readHashes_handlesEscapedQuotesInPaths() throws IOException {
        String json = """
                [
                    {"hash": "hash", "path": "/path/with\\"quotes\\".txt", "size": 100}
                ]
                """;
        Files.writeString(checksumFile, json);

        JSONChecksumReader reader = new JSONChecksumReader(checksumFile.toString());
        Map<String, String> hashes = reader.readHashes();

        assertEquals("hash", hashes.get("/path/with\"quotes\".txt"));
    }

    @Test
    void readHashes_handlesWindowsPathsWithBackslashes() throws IOException {
        String json = """
                [
                    {"hash": "hash", "path": "C:\\\\Users\\\\test\\\\file.txt", "size": 100}
                ]
                """;
        Files.writeString(checksumFile, json);

        JSONChecksumReader reader = new JSONChecksumReader(checksumFile.toString());
        Map<String, String> hashes = reader.readHashes();

        assertEquals("hash", hashes.get("C:\\Users\\test\\file.txt"));
    }

    @Test
    void readHashes_handlesSHA256Hash() throws IOException {
        String sha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        String json = String.format("""
                [
                    {"hash": "%s", "path": "/file.txt", "size": 100}
                ]
                """, sha256);
        Files.writeString(checksumFile, json);

        JSONChecksumReader reader = new JSONChecksumReader(checksumFile.toString());
        Map<String, String> hashes = reader.readHashes();

        assertEquals(sha256, hashes.get("/file.txt"));
    }

    @Test
    void readHashes_overwritesDuplicatePaths_withLastValue() throws IOException {
        String json = """
                [
                    {"hash": "hash1", "path": "/file.txt", "size": 100},
                    {"hash": "hash2", "path": "/file.txt", "size": 200}
                ]
                """;
        Files.writeString(checksumFile, json);

        JSONChecksumReader reader = new JSONChecksumReader(checksumFile.toString());
        Map<String, String> hashes = reader.readHashes();

        assertEquals(1, hashes.size());
        assertEquals("hash2", hashes.get("/file.txt"));
    }

    @Test
    void readHashes_handlesCompactJsonFormat() throws IOException {
        String json = "[{\"hash\":\"h1\",\"path\":\"/f1.txt\",\"size\":1},{\"hash\":\"h2\",\"path\":\"/f2.txt\",\"size\":2}]";
        Files.writeString(checksumFile, json);

        JSONChecksumReader reader = new JSONChecksumReader(checksumFile.toString());
        Map<String, String> hashes = reader.readHashes();

        assertEquals(2, hashes.size());
        assertEquals("h1", hashes.get("/f1.txt"));
        assertEquals("h2", hashes.get("/f2.txt"));
    }

    @Test
    void readHashes_ignoresExtraFieldsInJson() throws IOException {
        String json = """
                [
                    {"hash": "hash1", "path": "/file.txt", "size": 100}
                ]
                """;
        Files.writeString(checksumFile, json);

        JSONChecksumReader reader = new JSONChecksumReader(checksumFile.toString());
        Map<String, String> hashes = reader.readHashes();

        assertEquals("hash1", hashes.get("/file.txt"));
    }

    @Test
    void readHashes_throwsFileSystemException_forNonExistentFile() {
        JSONChecksumReader reader = new JSONChecksumReader("/non/existent/file.json");

        FileSystemException exception = assertThrows(
                FileSystemException.class,
                reader::readHashes
        );

        assertTrue(exception.getMessage().contains("/non/existent/file.json"));
    }

    @Test
    void readHashes_throwsChecksumFileException_forInvalidJson() throws IOException {
        Files.writeString(checksumFile, "not valid json");

        JSONChecksumReader reader = new JSONChecksumReader(checksumFile.toString());

        ChecksumFileException exception = assertThrows(
                ChecksumFileException.class,
                reader::readHashes
        );

        assertTrue(exception.getMessage().contains("Invalid JSON format"));
    }

    @Test
    void readHashes_throwsChecksumFileException_forJsonObjectInsteadOfArray() throws IOException {
        Files.writeString(checksumFile, "{\"hash\": \"h1\", \"path\": \"/f.txt\", \"size\": 1}");

        JSONChecksumReader reader = new JSONChecksumReader(checksumFile.toString());

        assertThrows(ChecksumFileException.class, reader::readHashes);
    }

    @Test
    void readHashes_throwsChecksumFileException_forEmptyFile() throws IOException {
        Files.writeString(checksumFile, "");

        JSONChecksumReader reader = new JSONChecksumReader(checksumFile.toString());

        assertThrows(ChecksumFileException.class, reader::readHashes);
    }

    @Test
    void readHashes_throwsFileSystemException_forDirectory() {
        JSONChecksumReader reader = new JSONChecksumReader(tempDir.toString());

        assertThrows(FileSystemException.class, reader::readHashes);
    }

    @Test
    void readHashes_handlesLargeNumberOfEntries() throws IOException {
        StringBuilder json = new StringBuilder("[");
        int count = 1000;
        for (int i = 0; i < count; i++) {
            if (i > 0) json.append(",");
            json.append(String.format(
                    "{\"hash\":\"hash%d\",\"path\":\"/file%d.txt\",\"size\":%d}",
                    i, i, i
            ));
        }
        json.append("]");
        Files.writeString(checksumFile, json.toString());

        JSONChecksumReader reader = new JSONChecksumReader(checksumFile.toString());
        Map<String, String> hashes = reader.readHashes();

        assertEquals(count, hashes.size());
    }
}