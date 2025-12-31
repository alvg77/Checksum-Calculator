package com.ChecksumCalculator.core.calculator;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SHA512Calculator extends ChecksumCalculator {
    @Override
    protected MessageDigest getMessageDigest() throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA-1");
    }
}
