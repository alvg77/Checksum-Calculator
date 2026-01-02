package com.ChecksumCalculator.calculator;

import com.ChecksumCalculator.exception.ChecksumCalculatorException;
import com.ChecksumCalculator.exception.FileSystemException;
import com.ChecksumCalculator.progress.observer.Observable;
import com.ChecksumCalculator.state.PauseController;
import com.ChecksumCalculator.state.PauseMessage;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

public abstract class ChecksumCalculator extends Observable {
    private static final int BUFFER_SIZE = 8192;
    private static final int PAUSE_CHECK_INTERVAL = (int) 1.049e+6;

    private final PauseController pauseController;

    public ChecksumCalculator(PauseController pauseController) {
        this.pauseController = pauseController;
    }

    public Optional<String> calculate(InputStream is) {
        byte[] buffer = new byte[BUFFER_SIZE];
        long tempRead = 0;

        try {
            MessageDigest md = getMessageDigest();
            int bytesRead;

            while ((bytesRead = is.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
                tempRead += bytesRead;
                notifyObservers(this, bytesRead);


                if (tempRead > PAUSE_CHECK_INTERVAL) {
                    tempRead = 0;
                    if (pauseController != null && pauseController.isPauseRequested()) {
                        notifyObservers(this, new PauseMessage());
                        return Optional.empty();
                    }
                }
            }

            return Optional.of(getHexString(md.digest()));
        } catch (NoSuchAlgorithmException e) {
            throw new ChecksumCalculatorException("The hashing algorithm is not supported", e);
        } catch (IOException e) {
            throw new FileSystemException("Error occurred while reading file. Could not calculate checksum.", e);
        }

    }

    public abstract Algorithm getAlgorithm();

    protected abstract MessageDigest getMessageDigest() throws NoSuchAlgorithmException;

    private static String getHexString(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();

        for (byte b : bytes) {
            hexString.append(String.format("%02x", b));
        }

        return hexString.toString();
    }
}
