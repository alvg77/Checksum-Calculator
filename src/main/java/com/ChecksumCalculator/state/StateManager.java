package com.ChecksumCalculator.state;

import com.ChecksumCalculator.exception.FileSystemException;

import java.io.*;

public class StateManager {
    public void save(Serializable memento, File file) {
        try (FileOutputStream fos = new FileOutputStream(file); ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(memento);
        } catch (IOException e) {
            throw new FileSystemException(String.format("Could not save state to %s", file.getAbsolutePath()), e);
        }
    }

    public Serializable load(File file) {
        try (FileInputStream fis = new FileInputStream(file);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            return (Serializable) ois.readObject();
        } catch (IOException e) {
            throw new FileSystemException(String.format("Cannot load memento from %s.", file.getPath()), e);
        } catch (ClassNotFoundException e) {
            throw new FileSystemException(String.format("Cannot load state: %s", e.getMessage()), e);
        }
    }
}