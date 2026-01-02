package com.ChecksumCalculator.state.memento;

import com.ChecksumCalculator.cli.Format;
import com.ChecksumCalculator.calculator.Algorithm;

import java.io.Serializable;

public record CalculationStateMemento(HashStreamWriterMemento hashStreamWriterMemento,
                                      ProgressReporterMemento progressReporterMemento,
                                      String path,
                                      String outputFilePath,
                                      Format format,
                                      Algorithm algorithm) implements Serializable {
}
