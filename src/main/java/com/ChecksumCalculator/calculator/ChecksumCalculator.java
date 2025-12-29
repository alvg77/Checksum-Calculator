package com.ChecksumCalculator.calculator;

import java.io.IOException;
import java.io.InputStream;

public interface ChecksumCalculator {
    public String calculate(InputStream is);
}
