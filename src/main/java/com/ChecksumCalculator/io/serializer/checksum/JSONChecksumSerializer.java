package com.ChecksumCalculator.io.serializer.checksum;

import com.ChecksumCalculator.exception.ChecksumFileException;
import com.ChecksumCalculator.exception.FileSystemException;
import com.ChecksumCalculator.io.ChecksumData;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;

public class JSONChecksumSerializer implements ChecksumSerializer {
    private final JsonGenerator generator;
    private boolean started = false;

    public JSONChecksumSerializer(OutputStream os) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            this.generator = mapper.getFactory().createGenerator(os);
        } catch (IOException e) {
            throw new ChecksumFileException("Failed to create JSON generator", e);
        }
        DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
        generator.setPrettyPrinter(printer);
    }

    @Override
    public void write(ChecksumData data) {
        try {
            if (!started) {
                generator.writeStartArray();
                started = true;
            }
            generator.writeObject(data);
        } catch (IOException e) {
            throw new FileSystemException(String.format("Error while writing checksum data: %s", e.getMessage()), e);
        }
    }

    @Override
    public void close() {
        try {
            if (started) {
                generator.writeEndArray();
            }
            generator.close();
        } catch (IOException e) {
            throw new FileSystemException(String.format("Error while closing JSON checksum serializer: %s", e.getMessage()), e);
        }
    }
}