package com.ChecksumCalculator.io.serializer.checksum;

import com.ChecksumCalculator.io.ChecksumData;

public interface ChecksumSerializer extends AutoCloseable {
    void write(ChecksumData data);

    @Override
    void close();
}