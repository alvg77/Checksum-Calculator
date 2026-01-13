package com.ChecksumCalculator.progress.observer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ObservableTest {

    private Observable observable;
    private Observer mockObserver1;
    private Observer mockObserver2;

    @BeforeEach
    void setUp() {
        observable = new Observable();
        mockObserver1 = mock(Observer.class);
        mockObserver2 = mock(Observer.class);
    }

    @Test
    void addObserver_registersObserver() {
        observable.addObserver(mockObserver1);
        observable.notifyObservers(observable, "test");

        verify(mockObserver1).update(observable, "test");
    }

    @Test
    void addObserver_registersMultipleObservers() {
        observable.addObserver(mockObserver1);
        observable.addObserver(mockObserver2);
        observable.notifyObservers(observable, "test");

        verify(mockObserver1).update(observable, "test");
        verify(mockObserver2).update(observable, "test");
    }

    @Test
    void addObserver_allowsDuplicateRegistration() {
        observable.addObserver(mockObserver1);
        observable.addObserver(mockObserver1);
        observable.notifyObservers(observable, "test");

        verify(mockObserver1, times(2)).update(observable, "test");
    }

    @Test
    void removeObserver_unregistersObserver() {
        observable.addObserver(mockObserver1);
        observable.addObserver(mockObserver2);
        observable.removeObserver(mockObserver1);
        observable.notifyObservers(observable, "test");

        verify(mockObserver1, never()).update(any(), any());
        verify(mockObserver2).update(observable, "test");
    }

    @Test
    void removeObserver_removesOnlyFirstOccurrence() {
        observable.addObserver(mockObserver1);
        observable.addObserver(mockObserver1);
        observable.removeObserver(mockObserver1);
        observable.notifyObservers(observable, "test");

        verify(mockObserver1, times(1)).update(observable, "test");
    }

    @Test
    void removeObserver_handlesNonExistentObserver() {
        observable.addObserver(mockObserver1);

        assertDoesNotThrow(() -> observable.removeObserver(mockObserver2));

        observable.notifyObservers(observable, "test");
        verify(mockObserver1).update(observable, "test");
    }

    @Test
    void notifyObservers_passesSourceAndMessage() {
        observable.addObserver(mockObserver1);

        Observable source = new Observable();
        observable.notifyObservers(source, "message");

        verify(mockObserver1).update(source, "message");
    }

    @Test
    void notifyObservers_notifiesInOrderOfAddition() {
        List<Integer> order = new ArrayList<>();

        observable.addObserver((s, m) -> order.add(1));
        observable.addObserver((s, m) -> order.add(2));
        observable.addObserver((s, m) -> order.add(3));
        observable.notifyObservers(observable, "test");

        assertEquals(List.of(1, 2, 3), order);
    }

    @Test
    void notifyObservers_handlesNoObservers() {
        assertDoesNotThrow(() -> observable.notifyObservers(observable, "test"));
    }

    @Test
    void notifyObservers_propagatesObserverException() {
        Observer throwingObserver = (s, m) -> {
            throw new RuntimeException("Observer error");
        };

        observable.addObserver(throwingObserver);

        assertThrows(RuntimeException.class, () ->
                observable.notifyObservers(observable, "test")
        );
    }

    @Test
    void typicalWorkflow_addsNotifiesAndRemoves() {
        List<String> received = new ArrayList<>();
        Observer logObserver = (s, m) -> received.add((String) m);

        observable.addObserver(logObserver);
        observable.notifyObservers(observable, "event1");
        observable.notifyObservers(observable, "event2");
        observable.removeObserver(logObserver);
        observable.notifyObservers(observable, "event3");

        assertEquals(List.of("event1", "event2"), received);
    }

    @Test
    void subclass_canNotifyObservers() {
        class ConcreteObservable extends Observable {
            void doSomething() {
                notifyObservers(this, "something happened");
            }
        }

        ConcreteObservable concrete = new ConcreteObservable();
        concrete.addObserver(mockObserver1);
        concrete.doSomething();

        verify(mockObserver1).update(concrete, "something happened");
    }
}