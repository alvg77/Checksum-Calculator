package com.ChecksumCalculator.io.serializer;

import com.ChecksumCalculator.filesystem.visitor.Status;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;

public class ComparisonResultSerializer implements AutoCloseable {
    private final PrintWriter writer;

    public ComparisonResultSerializer(OutputStream os) {
        this.writer = new PrintWriter(os);
    }

    public void write(Map<String, Status> results) {
        IO.println();
        IO.println("======REPORT======");
        for (Map.Entry<String, Status> entry : results.entrySet()) {
            writer.println(entry.getKey() + ": " + entry.getValue());
        }
    }

    public void close() {
        writer.close();
    }
}