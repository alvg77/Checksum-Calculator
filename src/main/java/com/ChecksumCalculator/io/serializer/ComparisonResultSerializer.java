package com.ChecksumCalculator.io.serializer;

import com.ChecksumCalculator.core.verification.Status;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;

public class ComparisonResultSerializer {
    private final PrintWriter writer;

    public ComparisonResultSerializer(OutputStream os) {
        this.writer = new PrintWriter(os, true);
    }

    public void write(Map<String, Status> results) {
        IO.println();
        IO.println("======REPORT======");
        for (Map.Entry<String, Status> entry : results.entrySet()) {
            writer.println(entry.getKey() + ": " + entry.getValue());
        }
    }
}