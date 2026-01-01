package com.ChecksumCalculator.state.memento;

import java.io.Serializable;

public record ProgressReporterMemento(long bytesCompleted) implements Serializable {
}