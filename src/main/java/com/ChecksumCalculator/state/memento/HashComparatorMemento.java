package com.ChecksumCalculator.state.memento;

import com.ChecksumCalculator.core.verification.Status;

import java.io.Serializable;
import java.util.Map;

public record HashComparatorMemento(Map<String, Status> statuses) implements Serializable { }
