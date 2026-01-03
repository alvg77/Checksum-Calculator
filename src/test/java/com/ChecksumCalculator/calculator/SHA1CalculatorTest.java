package com.ChecksumCalculator.calculator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SHA1CalculatorTest extends ChecksumCalculatorTestBase {
    @ParameterizedTest(name = "SHA1 of \"{0}\"")
    @CsvSource({
            "testing, dc724af18fbdd4e59189f5fe768a5f8311527050",
            "'Hello, World!', 0a0a9f2a6772942557ab5355d76af442f8f65e01",
            "'', da39a3ee5e6b4b0d3255bfef95601890afd80709",
            "' ', b858cb282617fb0956d960215c8e84d1ccf909c6"
    })
    void calculate_returnsCorrectHash(String input, String expectedHash) {
        SHA1Calculator calculator = new SHA1Calculator(pauseController);

        Optional<String> result = calculator.calculate(toInputStream(input));

        assertTrue(result.isPresent());
        assertEquals(expectedHash, result.get());
    }

    @Test
    void getAlgorithm_returnsSHA1() {
        SHA1Calculator calculator = new SHA1Calculator(pauseController);

        assertEquals(Algorithm.SHA1, calculator.getAlgorithm());
    }
}
