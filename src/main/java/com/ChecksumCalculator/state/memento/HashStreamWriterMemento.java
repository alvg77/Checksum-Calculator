package com.ChecksumCalculator.state.memento;

import java.io.Serializable;
import java.util.Set;

public record HashStreamWriterMemento(Set<String> processedFiles) implements Serializable {
}