package com.ChecksumCalculator.core.calculator;

import com.ChecksumCalculator.state.PauseController;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Calculator extends ChecksumCalculator {
    public MD5Calculator(PauseController pauseController) {
        super(pauseController);
    }

    @Override
    protected MessageDigest getMessageDigest() throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("MD5");
    }

    @Override
    public Algorithm getAlgorithm() {
        return Algorithm.MD5;
    }
}
