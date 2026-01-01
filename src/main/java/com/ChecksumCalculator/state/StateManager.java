package com.ChecksumCalculator.state;

import com.ChecksumCalculator.state.memento.CalculationStateMemento;

import java.io.*;

public class StateManager {
    public void save(Serializable memento, File file) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(memento);
        } catch (IOException e) {
            throw new RuntimeException("Cannot save state: " + e.getMessage(), e);
        }
    }

    public Serializable load(File file) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (Serializable) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Cannot load state: " + e.getMessage(), e);
        }
    }
}