package com.ChecksumCalculator.core.calculator;

import com.ChecksumCalculator.cli.CLIErrorHandler;
import com.ChecksumCalculator.progress.observer.Observable;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public abstract class ChecksumCalculator extends Observable {
    public String calculate(InputStream is) {
        byte[] buffer = new byte[4096];
        String checksum = "";

        try {
            MessageDigest md = getMessageDigest();

            int rc = is.read(buffer);
            while (rc != -1) {
                notifyObservers(this, rc);
                md.update(buffer, 0, rc);
                rc = is.read(buffer);
            }

            checksum = getHexString(md.digest());
        } catch (NoSuchAlgorithmException | IOException e) {
            CLIErrorHandler.fatal(e.getMessage());
        }

        return checksum;
    }

    private static String getHexString(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();

        for (byte b : bytes) {
            hexString.append(String.format("%02x", b));
        }

        return hexString.toString();
    }

    protected abstract MessageDigest getMessageDigest() throws NoSuchAlgorithmException;
}
