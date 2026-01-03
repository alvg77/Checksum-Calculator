package com.ChecksumCalculator.calculator;

import com.ChecksumCalculator.progress.observer.Observable;
import com.ChecksumCalculator.progress.observer.Observer;
import com.ChecksumCalculator.state.PauseController;
import org.junit.jupiter.api.BeforeEach;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;

public abstract class ChecksumCalculatorTestBase {
    protected PauseController pauseController;
    protected TestObserver observer;

    @BeforeEach
    void setUpBase() {
        pauseController = mock(PauseController.class);
        observer = new TestObserver();
    }

    protected InputStream toInputStream(String string) {
        return new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8));
    }

    protected InputStream toInputStream(byte[] bytes) {
        return new ByteArrayInputStream(bytes);
    }

    protected static class TestObserver implements Observer {
        public final List<Object> messages = new ArrayList<>();
        public long totalBytesReported = 0;

        @Override
        public void update(Observable source, Object message) {
            messages.add(message);
            if (message instanceof Integer) {
                totalBytesReported += (Integer) message;
            }
        }
    }
}
