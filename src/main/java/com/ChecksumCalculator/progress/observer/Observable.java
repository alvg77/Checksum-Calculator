package com.ChecksumCalculator.progress.observer;

import java.util.ArrayList;
import java.util.List;

public class Observable {
    List<Observer> observers;

    public Observable() {
        observers = new ArrayList<>();
    }

    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    public void notifyObservers(Observable source, Object msg) {
        for (Observer observer : observers) {
            observer.update(source, msg);
        }
    }
}
