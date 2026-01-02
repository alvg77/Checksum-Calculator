package com.ChecksumCalculator.state;

import com.ChecksumCalculator.exception.FileSystemException;

import java.io.*;

public class StateManager {
    public void save(Serializable memento, File file) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(memento);
        } catch (IOException e) {
            throw new FileSystemException(String.format("Could not save state to %s", file.getAbsolutePath()), e);
        }
    }

    public Serializable load(File file) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (Serializable) ois.readObject();
        } catch (IOException e) {
            throw new FileSystemException(String.format("Cannot load memento from %s.", file.getPath()), e);
        } catch (ClassNotFoundException e) {
            throw new FileSystemException(String.format("Cannot load state: %s", e.getMessage()), e);
        }
    }
}