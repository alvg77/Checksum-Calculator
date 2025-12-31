package com.ChecksumCalculator.io.serializer.checksum;

import com.ChecksumCalculator.io.ChecksumData;

import java.io.OutputStream;
import java.io.PrintWriter;

public class TextChecksumSerializer implements ChecksumSerializer {
    private PrintWriter writer;

    public TextChecksumSerializer(OutputStream outputStream) {
        writer = new PrintWriter(outputStream);
    }

    @Override
    public void write(ChecksumData data) {
        writer.println(data.hash() + " " + data.path() + " " + data.size());
    }

    @Override
    public void close() {
        writer.close();
    }
}
