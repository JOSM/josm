// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.testutils.annotations.MapStyles;
import org.openstreetmap.josm.testutils.annotations.Presets;
import org.openstreetmap.josm.tools.Logging;

/**
 * Unit tests for class {@link PostDownloadHandler}.
 */
@MapStyles
@Presets
class PostDownloadHandlerTest {
    private static DownloadTask newTask(final List<Object> errorObjects) {
        return new DownloadTask() {
            @Override
            public Future<?> loadUrl(DownloadParams settings, String url, ProgressMonitor progressMonitor) {
                return null;
            }

            @Override
            public String getTitle() {
                return null;
            }

            @Override
            public String[] getPatterns() {
                return new String[0];
            }

            @Override
            public List<Object> getErrorObjects() {
                return errorObjects;
            }

            @Override
            public String getConfirmationMessage(URL url) {
                return null;
            }

            @Override
            public Future<?> download(DownloadParams settings, Bounds downloadArea, ProgressMonitor progressMonitor) {
                return null;
            }

            @Override
            public void cancel() {
            }

            @Override
            public boolean acceptsUrl(String url, boolean isRemotecontrol) {
                return false;
            }

            @Override
            public String acceptsDocumentationSummary() {
                return null;
            }

            @Override
            public void setZoomAfterDownload(boolean zoomAfterDownload) {
                // Do nothing
            }
        };
    }

    private static Future<Object> newFuture(final String exceptionName) {
        return new Future<Object>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return false;
            }

            @Override
            public Object get() throws InterruptedException, ExecutionException {
                if (exceptionName != null) {
                    throw new ExecutionException(exceptionName, null);
                }
                return null;
            }

            @Override
            public Object get(long timeout, TimeUnit unit)
                    throws InterruptedException, ExecutionException, TimeoutException {
                return null;
            }
        };
    }

    /**
     * Unit test of {@code PostDownloadHandler#run} - error case: future throws exception.
     */
    @Test
    void testRunExceptionFuture() {
        Logging.clearLastErrorAndWarnings();
        new PostDownloadHandler(null, newFuture("testRunExceptionFuture")).run();
        assertTrue(
                Logging.getLastErrorAndWarnings().stream()
                        .anyMatch(e -> e.endsWith("E: java.util.concurrent.ExecutionException: testRunExceptionFuture")),
                        Logging.getLastErrorAndWarnings().toString());
    }

    /**
     * Unit test of {@code PostDownloadHandler#run} - nominal case: no errors.
     */
    @Test
    void testRunNoError() {
        Logging.clearLastErrorAndWarnings();
        new PostDownloadHandler(newTask(Collections.emptyList()), newFuture(null)).run();
        assertTrue(Logging.getLastErrorAndWarnings().isEmpty(), Logging.getLastErrorAndWarnings().toString());
    }

    /**
     * Unit test of {@code PostDownloadHandler#run} - nominal case: only one error.
     */
    @Test
    void testRunOneError() {
        Logging.clearLastErrorAndWarnings();
        new PostDownloadHandler(newTask(Collections.singletonList(new Object())), newFuture(null)).run();
        assertTrue(Logging.getLastErrorAndWarnings().isEmpty(), Logging.getLastErrorAndWarnings().toString());
    }

    /**
     * Unit test of {@code PostDownloadHandler#run} - nominal case: multiple errors.
     */
    @Test
    void testRunMultipleErrors() {
        Logging.clearLastErrorAndWarnings();
        new PostDownloadHandler(newTask(Arrays.asList("foo", new Exception("bar"), new Object())), newFuture(null)).run();
        assertTrue(
                Logging.getLastErrorAndWarnings().stream()
                        .anyMatch(e -> e.endsWith("E: java.lang.Exception: bar")), Logging.getLastErrorAndWarnings().toString());
    }
}
