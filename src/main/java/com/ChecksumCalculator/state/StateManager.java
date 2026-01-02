package com.ChecksumCalculator.state;

import java.io.*;

public class StateManager {
    public void save(Serializable memento, File file) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(memento);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Serializable load(File file) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (Serializable) ois.readObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Cannot load state: " + e.getMessage(), e);
        }
    }
}