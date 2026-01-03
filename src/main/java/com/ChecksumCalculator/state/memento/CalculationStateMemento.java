package com.ChecksumCalculator.state.memento;

import com.ChecksumCalculator.calculator.Algorithm;
import com.ChecksumCalculator.cli.Format;

import java.io.Serializable;

public record CalculationStateMemento(HashStreamWriterMemento hashStreamWriterMemento,
                                      ProgressReporterMemento progressReporterMemento,
                                      String path,
                                      String outputFilePath,
                                      Format format,
                                      Algorithm algorithm) implements Serializable {
}
