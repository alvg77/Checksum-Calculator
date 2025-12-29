package com.ChecksumCalculator.calculator;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.io.InputStream;

public class SHA512Calculator implements ChecksumCalculator {
    @Override
    public String calculate(InputStream is) {
        try {
            return DigestUtils.shake256_512Hex(is);
        } catch (IOException e) {
            throw new RuntimeException(e); // TODO:
        }
    }
}
