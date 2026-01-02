package com.ChecksumCalculator.progress;

import com.ChecksumCalculator.calculator.ChecksumCalculator;
import com.ChecksumCalculator.filesystem.visitor.HashComparator;
import com.ChecksumCalculator.filesystem.visitor.HashStreamWriter;
import com.ChecksumCalculator.progress.observer.Observable;
import com.ChecksumCalculator.progress.observer.Observer;
import com.ChecksumCalculator.state.PauseMessage;
import com.ChecksumCalculator.state.memento.ProgressReporterMemento;

public class ProgressReporter implements Observer {
    private String currentPath;
    private long bytesReadCurrentFile;
    private long bytesCompleted;
    private long bytesReadSum;
    private final long totalBytes;
    private long startTimeMillis;
    private int remainingSeconds;

    public ProgressReporter(long totalBytes) {
        this.currentPath = "(nothing)";
        this.bytesReadCurrentFile = 0;
        this.bytesCompleted = 0;
        this.bytesReadSum = 0;
        this.totalBytes = totalBytes;
        this.startTimeMillis = System.currentTimeMillis();
    }

    @Override
    public void update(Observable source, Object msg) {
        if (source instanceof ChecksumCalculator) {
            if (msg instanceof PauseMessage) {
                return;
            } else if (msg instanceof Number) {
                long bytes = ((Number) msg).longValue();
                bytesReadCurrentFile += bytes;
                bytesReadSum += bytes;

                long elapsedMillis = System.currentTimeMillis() - startTimeMillis;
                if (elapsedMillis > 0) {
                    long bytesPerSecond = (bytesReadSum * 1000) / elapsedMillis;
                    if (bytesPerSecond > 0) {
                        remainingSeconds = (int) ((totalBytes - bytesCompleted - bytesReadCurrentFile) / bytesPerSecond);
                    }
                }
            } else {
                throw new IllegalArgumentException("Unknown message type");
            }
        } else if (source instanceof HashStreamWriter || source instanceof HashComparator) {
            if (msg instanceof String) {
                bytesCompleted += bytesReadCurrentFile;
                currentPath = (String) msg;
                bytesReadCurrentFile = 0;
            } else {
                throw new IllegalArgumentException("Unknown message type");
            }
        } else {
            throw new IllegalArgumentException("Unknown message source");
        }

        refreshDisplay();
    }

    public ProgressReporterMemento saveToMemento() {
        return new ProgressReporterMemento(bytesCompleted);
    }

    public void restoreFromMemento(ProgressReporterMemento memento) {
        bytesCompleted = memento.bytesCompleted();
        bytesReadSum = 0;
        startTimeMillis = System.currentTimeMillis();
    }

    private void refreshDisplay() {
        StringBuilder sb = new StringBuilder();
        sb.append("Processing ");
        sb.append(currentPath);
        sb.append(" ... ");
        sb.append(bytesReadCurrentFile);
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
