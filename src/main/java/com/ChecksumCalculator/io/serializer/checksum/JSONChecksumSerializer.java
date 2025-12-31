package com.ChecksumCalculator.io.serializer.checksum;

import com.ChecksumCalculator.cli.CLIErrorHandler;
import com.ChecksumCalculator.io.ChecksumData;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;

public class JSONChecksumSerializer implements ChecksumSerializer {
    private final JsonGenerator generator;
    private boolean started = false;

    public JSONChecksumSerializer(OutputStream os) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        this.generator = mapper.getFactory().createGenerator(os);
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
            CLIErrorHandler.fatal(e.getMessage(), CLIErrorHandler.ErrorType.IO);
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
            CLIErrorHandler.fatal(e.getMessage(), CLIErrorHandler.ErrorType.IO);
        }
    }
}