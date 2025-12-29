package com.ChecksumCalculator.calculator;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.io.InputStream;

public class SHA1Calculator implements ChecksumCalculator {
    @Override
    public String calculate(InputStream is) {
        try {
            return DigestUtils.sha1Hex(is);
        } catch (IOException e) {
            throw new RuntimeException(e); // TODO:
        }
    }
}
