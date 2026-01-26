package com.ChecksumCalculator.progress.observer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ObservableTest {
    private TestableObservable observable;
    private Observer mockObserver;

    private static class TestableObservable extends Observable {
        public void triggerNotification(Observable source, Object message) {
            notifyObservers(source, message);
        }

        public void triggerNotification(Object message) {
            notifyObservers(this, message);
        }
    }

    @BeforeEach
    void setUp() {
        observable = new TestableObservable();
        mockObserver = mock(Observer.class);
    }

    @Test
    void addObserver_registeredObserverReceivesNotifications() {
        observable.addObserver(mockObserver);

        observable.triggerNotification("event");

        verify(mockObserver).update(observable, "event");
    }

    @Test
    void addObserver_multipleObserversAllReceiveNotifications() {
        Observer secondObserver = mock(Observer.class);

        observable.addObserver(mockObserver);
        observable.addObserver(secondObserver);
        observable.triggerNotification("event");

        verify(mockObserver).update(observable, "event");
        verify(secondObserver).update(observable, "event");
    }

    @Test
    void addObserver_observersNotifiedInRegistrationOrder() {
        List<Integer> callOrder = new ArrayList<>();

        observable.addObserver((src, msg) -> callOrder.add(1));
        observable.addObserver((src, msg) -> callOrder.add(2));
        observable.addObserver((src, msg) -> callOrder.add(3));
        observable.triggerNotification("event");

        assertEquals(List.of(1, 2, 3), callOrder);
    }

    @Test
    void removeObserver_removedObserverNoLongerReceivesNotifications() {
        observable.addObserver(mockObserver);
        observable.removeObserver(mockObserver);

        observable.triggerNotification("event");

        verify(mockObserver, never()).update(any(), any());
    }

    @Test
    void removeObserver_removingNonExistentObserverDoesNothing() {
        Observer otherObserver = mock(Observer.class);
        observable.addObserver(mockObserver);

        assertDoesNotThrow(() -> observable.removeObserver(otherObserver));

        observable.triggerNotification("event");
        verify(mockObserver).update(observable, "event");
    }

    @Test
    void removeObserver_onlySpecifiedObserverIsRemoved() {
        Observer keepObserver = mock(Observer.class);

        observable.addObserver(mockObserver);
        observable.addObserver(keepObserver);
        observable.removeObserver(mockObserver);
        observable.triggerNotification("event");

        verify(mockObserver, never()).update(any(), any());
        verify(keepObserver).update(observable, "event");
    }

    @Test
    void notifyObservers_passesCorrectSourceAndMessage() {
        TestableObservable customSource = new TestableObservable();
        Object message = new Object();

        observable.addObserver(mockObserver);
        observable.triggerNotification(customSource, message);

        verify(mockObserver).update(customSource, message);
    }

    @Test
    void notifyObservers_handlesEmptyObserverList() {
        assertDoesNotThrow(() -> observable.triggerNotification("event"));
    }

    @Test
    void notifyObservers_exceptionInObserverPropagatesToCaller() {
        RuntimeException expectedException = new RuntimeException("Observer failed");
        Observer failingObserver = (src, msg) -> {
            throw expectedException;
        };

        observable.addObserver(failingObserver);

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> observable.triggerNotification("event")
        );
        assertSame(expectedException, thrown);
    }


    @Test
    void typicalUsage_observerReceivesMultipleNotifications() {
        List<String> receivedMessages = new ArrayList<>();
        observable.addObserver((src, msg) -> receivedMessages.add((String) msg));

        observable.triggerNotification("first");
        observable.triggerNotification("second");
        observable.triggerNotification("third");

        assertEquals(List.of("first", "second", "third"), receivedMessages);
    }

    @Test
    void typicalUsage_observerStopsReceivingAfterRemoval() {
        List<String> receivedMessages = new ArrayList<>();
        Observer trackingObserver = (src, msg) -> receivedMessages.add((String) msg);

        observable.addObserver(trackingObserver);
        observable.triggerNotification("before");
        observable.removeObserver(trackingObserver);
        observable.triggerNotification("after");

        assertEquals(List.of("before"), receivedMessages);
    }
}