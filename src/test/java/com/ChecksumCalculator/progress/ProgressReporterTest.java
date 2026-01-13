package com.ChecksumCalculator.progress;

import com.ChecksumCalculator.calculator.ChecksumCalculator;
import com.ChecksumCalculator.filesystem.visitor.HashComparator;
import com.ChecksumCalculator.filesystem.visitor.HashStreamWriter;
import com.ChecksumCalculator.progress.observer.Observable;
import com.ChecksumCalculator.state.PauseMessage;
import com.ChecksumCalculator.state.memento.ProgressReporterMemento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProgressReporterTest {

    private ProgressReporter reporter;
    private ChecksumCalculator mockCalculator;
    private HashStreamWriter mockWriter;
    private HashComparator mockComparator;

    @BeforeEach
    void setUp() {
        reporter = new ProgressReporter(1000L);
        mockCalculator = mock(ChecksumCalculator.class);
        mockWriter = mock(HashStreamWriter.class);
        mockComparator = mock(HashComparator.class);
    }

    @Test
    void update_accumulatesBytesFromCalculator() {
        reporter.update(mockCalculator, 100L);
        reporter.update(mockCalculator, 200L);
        reporter.update(mockWriter, "/file.txt");

        ProgressReporterMemento memento = reporter.saveToMemento();
        assertEquals(300L, memento.bytesCompleted());
    }

    @Test
    void update_handlesIntegerMessage() {
        reporter.update(mockCalculator, Integer.valueOf(500));
        reporter.update(mockWriter, "/file.txt");

        ProgressReporterMemento memento = reporter.saveToMemento();
        assertEquals(500L, memento.bytesCompleted());
    }

    @Test
    void update_ignoresPauseMessageFromCalculator() {
        reporter.update(mockCalculator, 100L);
        reporter.update(mockCalculator, mock(PauseMessage.class));
        reporter.update(mockCalculator, 100L);
        reporter.update(mockWriter, "/file.txt");

        ProgressReporterMemento memento = reporter.saveToMemento();
        assertEquals(200L, memento.bytesCompleted());
    }

    @Test
    void update_throwsForUnknownMessageFromCalculator() {
        assertThrows(IllegalArgumentException.class, () ->
                reporter.update(mockCalculator, "invalid message")
        );
    }

    @Test
    void update_completesFileOnWriterStringMessage() {
        reporter.update(mockCalculator, 500L);
        reporter.update(mockWriter, "/path/to/file.txt");

        ProgressReporterMemento memento = reporter.saveToMemento();
        assertEquals(500L, memento.bytesCompleted());
    }

    @Test
    void update_accumulatesBytesAcrossMultipleFiles() {
        for (int i = 0; i < 5; i++) {
            reporter.update(mockCalculator, 100L);
            reporter.update(mockWriter, "/file" + i + ".txt");
        }

        ProgressReporterMemento memento = reporter.saveToMemento();
        assertEquals(500L, memento.bytesCompleted());
    }

    @Test
    void update_throwsForNonStringMessageFromWriter() {
        assertThrows(IllegalArgumentException.class, () ->
                reporter.update(mockWriter, 123L)
        );
    }

    @Test
    void update_handlesHashComparatorSameAsWriter() {
        reporter.update(mockCalculator, 500L);
        reporter.update(mockComparator, "/compared/file.txt");

        ProgressReporterMemento memento = reporter.saveToMemento();
        assertEquals(500L, memento.bytesCompleted());
    }

    @Test
    void update_throwsForUnknownSource() {
        Observable unknownSource = mock(Observable.class);

        assertThrows(IllegalArgumentException.class, () ->
                reporter.update(unknownSource, "message")
        );
    }

    @Test
    void saveToMemento_capturesBytesCompleted() {
        reporter.update(mockCalculator, 250L);
        reporter.update(mockWriter, "/file.txt");

        ProgressReporterMemento memento = reporter.saveToMemento();

        assertEquals(250L, memento.bytesCompleted());
    }

    @Test
    void saveToMemento_returnsZeroInitially() {
        ProgressReporterMemento memento = reporter.saveToMemento();

        assertEquals(0L, memento.bytesCompleted());
    }

    @Test
    void saveToMemento_excludesInProgressFileBytes() {
        reporter.update(mockCalculator, 100L);
        // No file completion

        ProgressReporterMemento memento = reporter.saveToMemento();

        assertEquals(0L, memento.bytesCompleted());
    }

    @Test
    void saveToMemento_createsIndependentSnapshots() {
        reporter.update(mockCalculator, 100L);
        reporter.update(mockWriter, "/file1.txt");
        ProgressReporterMemento memento1 = reporter.saveToMemento();

        reporter.update(mockCalculator, 200L);
        reporter.update(mockWriter, "/file2.txt");
        ProgressReporterMemento memento2 = reporter.saveToMemento();

        assertEquals(100L, memento1.bytesCompleted());
        assertEquals(300L, memento2.bytesCompleted());
    }

    @Test
    void restoreFromMemento_restoresBytesCompleted() {
        reporter.update(mockCalculator, 500L);
        reporter.update(mockWriter, "/file.txt");
        ProgressReporterMemento memento = reporter.saveToMemento();

        ProgressReporter newReporter = new ProgressReporter(1000L);
        newReporter.restoreFromMemento(memento);

        assertEquals(500L, newReporter.saveToMemento().bytesCompleted());
    }

    @Test
    void restoreFromMemento_resetsBytesReadSum() {
        reporter.update(mockCalculator, 100L);
        reporter.update(mockWriter, "/file1.txt");
        ProgressReporterMemento memento = reporter.saveToMemento();

        reporter.update(mockCalculator, 200L);
        reporter.update(mockWriter, "/file2.txt");

        reporter.restoreFromMemento(memento);

        reporter.update(mockCalculator, 50L);
        reporter.update(mockWriter, "/file3.txt");

        assertEquals(150L, reporter.saveToMemento().bytesCompleted());
    }

    @Test
    void restoreFromMemento_allowsMultipleRestores() {
        reporter.update(mockCalculator, 100L);
        reporter.update(mockWriter, "/file1.txt");
        ProgressReporterMemento memento1 = reporter.saveToMemento();

        reporter.update(mockCalculator, 200L);
        reporter.update(mockWriter, "/file2.txt");
        ProgressReporterMemento memento2 = reporter.saveToMemento();

        reporter.restoreFromMemento(memento1);
        assertEquals(100L, reporter.saveToMemento().bytesCompleted());

        reporter.restoreFromMemento(memento2);
        assertEquals(300L, reporter.saveToMemento().bytesCompleted());
    }

    @Test
    void update_handlesIncrementalByteUpdates() {
        for (int i = 0; i < 10; i++) {
            reporter.update(mockCalculator, 10L);
        }
        reporter.update(mockWriter, "/chunked-file.txt");

        ProgressReporterMemento memento = reporter.saveToMemento();
        assertEquals(100L, memento.bytesCompleted());
    }

    @Test
    void typicalWorkflow_calculateWritePauseResume() {
        reporter.update(mockCalculator, 100L);
        reporter.update(mockWriter, "/file1.txt");

        reporter.update(mockCalculator, 50L);
        reporter.update(mockCalculator, mock(PauseMessage.class));

        ProgressReporterMemento memento = reporter.saveToMemento();
        assertEquals(100L, memento.bytesCompleted());

        reporter.restoreFromMemento(memento);
        reporter.update(mockCalculator, 50L);
        reporter.update(mockWriter, "/file2.txt");

        assertEquals(150L, reporter.saveToMemento().bytesCompleted());
    }
}