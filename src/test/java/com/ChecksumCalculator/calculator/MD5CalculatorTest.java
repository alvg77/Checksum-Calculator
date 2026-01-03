package com.ChecksumCalculator.calculator;

import com.ChecksumCalculator.state.PauseMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class MD5CalculatorTest extends ChecksumCalculatorTestBase {

    @ParameterizedTest(name = "MD5 of \"{0}\"")
    @CsvSource({
            "testing, ae2b1fca515949e5d54fb22b8ed95575",
            "'Hello, World!', 65a8e27d8879283831b664bd8b7f0ad4",
            "'', d41d8cd98f00b204e9800998ecf8427e",
            "' ', 7215ee9c7d9dc229d2921a40e899ec5f"
    })
    void calculate_returnsCorrectHash(String input, String expectedHash) {
        MD5Calculator calculator = new MD5Calculator(pauseController);

        Optional<String> result = calculator.calculate(toInputStream(input));

        assertTrue(result.isPresent());

        assertEquals(expectedHash, result.get());
    }

    @Test
    void getAlgorithm_returnsMD5() {
        MD5Calculator calculator = new MD5Calculator(pauseController);

        assertEquals(Algorithm.MD5, calculator.getAlgorithm());
    }

    @Test
    void calculate_handlesBinaryData() {
        MD5Calculator calculator = new MD5Calculator(pauseController);
        byte[] binaryData = new byte[] {0x00, 0x01, (byte) 0xFF, (byte) 0xFE};

        Optional<String> result = calculator.calculate(new ByteArrayInputStream(binaryData));

        assertTrue(result.isPresent());

        assertEquals("50d2419ec6481e50c7dc209833437157", result.get());
    }

    @Test
    void calculate_handlesLargeInput() {
        MD5Calculator calculator = new MD5Calculator(pauseController);

        byte[] pattern = "1234567890".getBytes(StandardCharsets.UTF_8);
        byte[] largeData = new byte[5 * 1024 * 1024];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = pattern[i % pattern.length];
        }

        Optional<String> result = calculator.calculate(new ByteArrayInputStream(largeData));

        assertTrue(result.isPresent());

        assertEquals("8582e68e5c7e34f08e7fcde487715ae2", result.get());
    }

    @Test
    void calculate_notifiesObserverWithBytesRead() {
        MD5Calculator calculator = new MD5Calculator(pauseController);
        calculator.addObserver(observer);
        byte[] data = "Hello, World!".getBytes(StandardCharsets.UTF_8);

        calculator.calculate(toInputStream(data));

        assertEquals(data.length, observer.totalBytesReported);
    }

    @Test
    void calculate_notifiesObserverMultipleTimesForLargeInput() {
        MD5Calculator calculator = new MD5Calculator(pauseController);
        calculator.addObserver(observer);
        byte[] largeData = new byte[20000];

        calculator.calculate(toInputStream(largeData));

        assertEquals(largeData.length, observer.totalBytesReported);
        assertTrue(observer.messages.size() > 1, "Should notify multiple times for large input");
    }

    @Test
    void calculate_returnsNullWhenPauseRequested() {
        MD5Calculator calculator = new MD5Calculator(pauseController);
        when(pauseController.isPauseRequested()).thenReturn(true);

        byte[] largeData = new byte[2 * 1024 * 1024];

        Optional<String> result = calculator.calculate(toInputStream(largeData));

        assertTrue(result.isEmpty());
    }

    @Test
    void calculate_sendsPauseMessageWhenPauseRequested() {
        MD5Calculator calculator = new MD5Calculator(pauseController);
        calculator.addObserver(observer);

        when(pauseController.isPauseRequested()).thenReturn(true);

        byte[] largeData = new byte[2 * 1024 * 1024];

        calculator.calculate(toInputStream(largeData));

        boolean hasPauseMessage = observer.messages.stream()
                .anyMatch(msg -> msg instanceof PauseMessage);

        assertTrue(hasPauseMessage, "Should notify observer with PauseMessage");
    }

    @Test
    void calculate_continuesWhenPauseNotRequested() {
        MD5Calculator calculator = new MD5Calculator(pauseController);

        when(pauseController.isPauseRequested()).thenReturn(false);

        byte[] largeData = new byte[2 * 1024 * 1024];

        Optional<String> result = calculator.calculate(toInputStream(largeData));

        assertTrue(result.isPresent());
        assertEquals(32, result.get().length());
    }

    @Test
    void calculate_handlesNullPauseController() {
        MD5Calculator calculator = new MD5Calculator(null);
        byte[] largeData = new byte[2 * 1024 * 1024];

        Optional<String> result = calculator.calculate(toInputStream(largeData));

        assertTrue(result.isPresent());
        assertEquals(32, result.get().length());
    }
}
