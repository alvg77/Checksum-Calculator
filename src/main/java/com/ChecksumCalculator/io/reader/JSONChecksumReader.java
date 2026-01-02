package com.ChecksumCalculator.io.reader;

import com.ChecksumCalculator.exception.ChecksumFileException;
import com.ChecksumCalculator.exception.FileSystemException;
import com.ChecksumCalculator.io.ChecksumData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JSONChecksumReader extends ChecksumReader {
    public JSONChecksumReader(String filePath) {
        super(filePath);
    }

    @Override
    public Map<String, String> readHashes() {
        ObjectMapper mapper = new ObjectMapper();
        List<ChecksumData> checksumDataList = null;

        try {
            checksumDataList = mapper.readValue(new File(filePath), new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new ChecksumFileException(String.format("Invalid JSON format in checksum file: %s", filePath), e);
        } catch (IOException e) {
            throw new FileSystemException(String.format("Cannot read checksum file: %s", filePath), e);
        }

        assert checksumDataList != null;

        Map<String, String> hashes = new HashMap<>();
        for (ChecksumData checksumData : checksumDataList) {
            hashes.put(checksumData.path(), checksumData.hash());
        }

        return hashes;
    }
}
