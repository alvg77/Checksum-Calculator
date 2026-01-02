package com.ChecksumCalculator.state.memento;

import com.ChecksumCalculator.filesystem.visitor.Status;

import java.io.Serializable;
import java.util.Map;

public record HashComparatorMemento(Map<String, Status> statuses) implements Serializable { }
