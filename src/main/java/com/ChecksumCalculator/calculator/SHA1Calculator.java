package com.ChecksumCalculator.calculator;

import com.ChecksumCalculator.state.PauseController;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SHA1Calculator extends ChecksumCalculator {
    public SHA1Calculator(PauseController pauseController) {
        super(pauseController);
    }

    @Override
    protected MessageDigest getMessageDigest() throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA-1");
    }

    @Override
    public Algorithm getAlgorithm() {
        return Algorithm.SHA1;
    }
}
