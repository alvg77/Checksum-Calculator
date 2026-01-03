package com.ChecksumCalculator.state.memento;

import com.ChecksumCalculator.calculator.Algorithm;
import com.ChecksumCalculator.cli.Format;

import java.io.Serializable;

public record VerificationStateMemento(HashComparatorMemento hashComparatorMemento,
                                       ProgressReporterMemento progressReporterMemento,
                                       String path,
                                       String inputFilePath,
                                       Format format,
                                       Algorithm algorithm) implements Serializable {
}
