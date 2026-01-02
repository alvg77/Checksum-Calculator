package com.ChecksumCalculator.core.calculator;

import com.ChecksumCalculator.state.PauseController;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SHA512Calculator extends ChecksumCalculator {
    public SHA512Calculator(PauseController pauseController) {
        super(pauseController);
    }

    @Override
    protected MessageDigest getMessageDigest() throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA-512");
    }

    @Override
    public Algorithm getAlgorithm() {
        return Algorithm.SHA512;
    }
}
