package com.ChecksumCalculator.progress;

import com.ChecksumCalculator.cli.CLIErrorHandler;
import com.ChecksumCalculator.core.calculator.ChecksumCalculator;
import com.ChecksumCalculator.core.verification.HashComparator;
import com.ChecksumCalculator.filesystem.visitor.HashStreamWriter;
import com.ChecksumCalculator.progress.observer.Observable;
import com.ChecksumCalculator.progress.observer.Observer;

import java.sql.Timestamp;

public class ProgressReporter implements Observer {
    private String currentPath;
    private Integer bytesRead;
    private long bytesReadSum;
    private final long totalBytes;
    private final Timestamp timestamp;
    private int remainingSeconds;

    public ProgressReporter(long totalBytes) {
        currentPath = "(nothing)";
        bytesRead = 0;
        bytesReadSum = 0;
        this.totalBytes = totalBytes;
        timestamp = new Timestamp(System.currentTimeMillis());
    }

    @Override
    public void update(Observable source, Object msg) {
        if (source instanceof ChecksumCalculator) {
            bytesRead = (Integer) msg;
            bytesReadSum += bytesRead;

            Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());

            if (timestamp != null) {
                int secDiff = (int) ((currentTimestamp.getTime() - timestamp.getTime()) / 1000);

                if (secDiff > 0) {
                    long bytesPerSecond = bytesReadSum / secDiff;
                    if (bytesPerSecond > 0) {
                        remainingSeconds = Math.toIntExact((totalBytes - bytesReadSum) / bytesPerSecond);
                    }
                }
            }
        } else if (source instanceof HashStreamWriter || source instanceof HashComparator) {
            IO.println("\n");
            currentPath = (String) msg;
            bytesRead = 0;
        } else {
            CLIErrorHandler.warn("unexpected message, continuing...");
        }

        refreshDisplay();
    }

    private void refreshDisplay() {
        StringBuilder sb = new StringBuilder();
        sb.append("\rProcessing ");
        sb.append(currentPath);
        sb.append(" ... ");
        sb.append(bytesRead);
        sb.append(" byte(s) read, ");

        if (remainingSeconds > 0) {
            sb.append(remainingSeconds);
            sb.append(" second(s) remaining.");
        } else {
            sb.append("<1 seconds remaining.");
        }

        IO.println(sb.toString());
    }
}
