package com.ChecksumCalculator.state;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PauseControllerTest {
    private PauseController controller;
    private InputStream originalSystemIn;

    @BeforeEach
    void setUp() {
        controller = new PauseController();
        originalSystemIn = System.in;
    }

    @AfterEach
    void tearDown() {
        System.setIn(originalSystemIn);
    }

    @Test
    void isPauseRequested_returnsFalseInitially() {
        assertFalse(controller.isPauseRequested());
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void startPauseListener_setsPauseRequested_whenLowercasePEntered() throws InterruptedException {
        System.setIn(new ByteArrayInputStream("p\n".getBytes()));

        controller.startPauseListener();
        Thread.sleep(100);

        assertTrue(controller.isPauseRequested());
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void startPauseListener_setsPauseRequested_whenUppercasePEntered() throws InterruptedException {
        System.setIn(new ByteArrayInputStream("P\n".getBytes()));

        controller.startPauseListener();
        Thread.sleep(100);

        assertTrue(controller.isPauseRequested());
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void startPauseListener_setsPauseRequested_whenPHasWhitespace() throws InterruptedException {
        System.setIn(new ByteArrayInputStream("  p  \n".getBytes()));

        controller.startPauseListener();
        Thread.sleep(100);

        assertTrue(controller.isPauseRequested());
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void startPauseListener_ignoresNonPInputAndContinues() throws InterruptedException {
        System.setIn(new ByteArrayInputStream("x\ny\nz\np\n".getBytes()));

        controller.startPauseListener();
        Thread.sleep(100);

        assertTrue(controller.isPauseRequested());
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void startPauseListener_doesNotSetPause_forOtherCharacters() throws InterruptedException {
        System.setIn(new ByteArrayInputStream("x\n".getBytes()));

        controller.startPauseListener();
        Thread.sleep(100);

        assertFalse(controller.isPauseRequested());
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void startPauseListener_doesNotSetPause_forPauseWord() throws InterruptedException {
        System.setIn(new ByteArrayInputStream("pause\n".getBytes()));

        controller.startPauseListener();
        Thread.sleep(100);

        assertFalse(controller.isPauseRequested());
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void startPauseListener_stopsListening_afterPReceived() throws InterruptedException {
        System.setIn(new ByteArrayInputStream("p\nmore\ninput\n".getBytes()));

        controller.startPauseListener();
        Thread.sleep(100);

        assertTrue(controller.isPauseRequested());
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void startPauseListener_handles_streamEndingWithoutP() throws InterruptedException {
        System.setIn(new ByteArrayInputStream("not\n".getBytes()));

        controller.startPauseListener();
        Thread.sleep(100);

        assertFalse(controller.isPauseRequested());
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void startPauseListener_handles_immediateStreamClose() throws InterruptedException {
        System.setIn(new ByteArrayInputStream(new byte[0]));

        controller.startPauseListener();
        Thread.sleep(100);

        assertFalse(controller.isPauseRequested());
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void differentInstances_haveIndependentState() throws InterruptedException {
        PauseController controller1 = new PauseController();
        PauseController controller2 = new PauseController();

        System.setIn(new ByteArrayInputStream("p\n".getBytes()));
        controller1.startPauseListener();
        Thread.sleep(100);

        assertTrue(controller1.isPauseRequested());
        assertFalse(controller2.isPauseRequested());
    }
}