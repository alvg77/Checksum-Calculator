package com.ChecksumCalculator.state;

import java.util.Scanner;

public class PauseController {
    private volatile boolean pauseRequested = false;

    public synchronized boolean isPauseRequested() {
        return pauseRequested;
    }

    public void startPauseListener() {
        Thread listenerThread = new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                if (scanner.hasNextLine()) {
                    String input = scanner.nextLine();
                    if ("p".equalsIgnoreCase(input.trim())) {
                        pauseRequested = true;
                        IO.println();
                        break;
                    }
                }
            }
        });

        listenerThread.setDaemon(true);
        listenerThread.start();
    }
}
