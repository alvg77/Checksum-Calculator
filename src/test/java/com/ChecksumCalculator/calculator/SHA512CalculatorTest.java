package com.ChecksumCalculator.calculator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SHA512CalculatorTest extends ChecksumCalculatorTestBase {
    @ParameterizedTest(name = "SHA512 of \"{0}\"")
    @CsvSource({
            "testing, 521b9ccefbcd14d179e7a1bb877752870a6d620938b28a66a107eac6e6805b9d0989f45b5730508041aa5e710847d439ea74cd312c9355f1f2dae08d40e41d50",
            "'Hello, World!', 374d794a95cdcfd8b35993185fef9ba368f160d8daf432d08ba9f1ed1e5abe6cc69291e0fa2fe0006a52570ef18c19def4e617c33ce52ef0a6e5fbe318cb0387",
            "'', cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e",
            "' ', f90ddd77e400dfe6a3fcf479b00b1ee29e7015c5bb8cd70f5f15b4886cc339275ff553fc8a053f8ddc7324f45168cffaf81f8c3ac93996f6536eef38e5e40768"
    })
    void calculate_returnsCorrectHash(String input, String expectedHash) {
        SHA512Calculator calculator = new SHA512Calculator(pauseController);

        Optional<String> result = calculator.calculate(toInputStream(input));

        assertTrue(result.isPresent());
        assertEquals(expectedHash, result.get());
    }

    @Test
    void getAlgorithm_returnsSHA512() {
        SHA512Calculator calculator = new SHA512Calculator(pauseController);

        assertEquals(Algorithm.SHA512, calculator.getAlgorithm());
    }
}