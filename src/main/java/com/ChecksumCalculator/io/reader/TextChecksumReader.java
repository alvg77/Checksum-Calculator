package com.ChecksumCalculator.io.reader;

import com.ChecksumCalculator.exception.FileSystemException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TextChecksumReader extends ChecksumReader {
    public TextChecksumReader(String filePath) {
        super(filePath);
    }

    @Override
    public Map<String, String> readHashes() {
        Map<String, String> hashes = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;

                int firstSpace = line.indexOf(' ');
                if (firstSpace == -1) continue;

                String hash = line.substring(0, firstSpace);
                String rest = line.substring(firstSpace + 1);

                int lastSpace = rest.lastIndexOf(' ');
                if (lastSpace == -1) continue;

                String path = rest.substring(0, lastSpace);

                hashes.put(path, hash);
            }
        } catch (IOException e) {
            throw new FileSystemException(String.format("Cannot read hashes from file: %s", filePath));
        }

        return hashes;
    }
}
