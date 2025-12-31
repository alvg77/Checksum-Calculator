package com.ChecksumCalculator.io.reader;

import java.util.Map;

public abstract class ChecksumReader {
    protected String filePath;

    public ChecksumReader(String filePath) {
        this.filePath = filePath;
    }

    public abstract Map<String, String> readHashes();
}
