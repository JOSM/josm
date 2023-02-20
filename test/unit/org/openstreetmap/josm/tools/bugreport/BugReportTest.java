// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.bugreport;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Handler;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.platform.commons.util.ReflectionUtils;
import org.openstreetmap.josm.actions.ShowStatusReportAction;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.tools.Logging;

/**
 * Tests the bug report class.
 * @author Michael Zangl
 */
// Preferences for the report text
@BasicPreferences
class BugReportTest {
    private static Handler[] handlers;

    @AfterAll
    static void cleanup() {
        // Clear queue
        new BugReport(BugReport.intercept(new NullPointerException())).getReportText("");
        Logging.clearLastErrorAndWarnings();
        for (Handler handler : handlers) {
            Logging.getLogger().addHandler(handler);
        }
    }

    @BeforeAll
    static void setup() {
        handlers = Logging.getLogger().getHandlers();
        // Avoid console spam
        for (Handler handler : handlers) {
            Logging.getLogger().removeHandler(handler);
        }
    }

    /**
     * Test {@link BugReport#getReportText}
     */
    @Test
    void testReportText() {
        ReportedException e = interceptInChildMethod(new IOException("test-exception-message"));
        e.put("test-key", "test-value");
        String text = new BugReport(e).getReportText(ShowStatusReportAction.getReportHeader());

        assertTrue(text.contains("test-exception-message"));
        assertTrue(text.contains("interceptInChildMethod"));
        assertTrue(text.contains("testReportText")); // stack trace
        assertTrue(text.contains("test-key: test-value"));
    }

    /**
     * Test {@link BugReport#intercept(Throwable)}
     */
    @Test
    void testIntercept() {
        IOException base = new IOException("test");
        ReportedException intercepted = interceptInChildMethod(base);
        assertEquals(intercepted.getCause(), base);

        StringWriter out = new StringWriter();
        intercepted.printReportDataTo(new PrintWriter(out));

        assertTrue(out.toString().contains("interceptInChildMethod")); // calling method.

        assertSame(intercepted, BugReport.intercept(intercepted));
    }

    private ReportedException interceptInChildMethod(IOException base) {
        return BugReport.intercept(base);
    }

    /**
     * Test {@link BugReport#getCallingMethod(int)}
     */
    @Test
    void testGetCallingMethod() {
        assertEquals("BugReportTest#testGetCallingMethod", BugReport.getCallingMethod(1));
        assertEquals("BugReportTest#testGetCallingMethod", testGetCallingMethod2());
        assertEquals("?", BugReport.getCallingMethod(100));
    }

    private String testGetCallingMethod2() {
        return BugReport.getCallingMethod(2);
    }

    @Test
    void testSuppressedExceptionsOrder() {
        final String methodName = "testSuppressedExceptionsOrder";
        BugReport.addSuppressedException(new NullPointerException(methodName));
        BugReport.addSuppressedException(new IllegalStateException(methodName));
        BugReport bugReport = new BugReport(BugReport.intercept(new IOException(methodName)));
        final String report = assertDoesNotThrow(() -> bugReport.getReportText(methodName));
        assertAll(() -> assertTrue(report.contains("NullPointerException")),
                () -> assertTrue(report.contains("IOException")),
                () -> assertTrue(report.contains("IllegalStateException")));
        int ioe = report.indexOf("IOException");
        int npe = report.indexOf("NullPointerException");
        int ise = report.indexOf("IllegalStateException");
        assertAll("Ordering of exceptions is wrong",
                () -> assertTrue(ioe < npe, "IOException should be reported before NullPointerException"),
                () -> assertTrue(npe < ise, "NullPointerException should be reported before IllegalStateException"));
    }

    static Stream<Arguments> testSuppressedExceptions() {
        return Stream.of(
                Arguments.of("GuiHelper::runInEDTAndWaitAndReturn",
                        (Consumer<Runnable>) r -> GuiHelper.runInEDTAndWaitAndReturn(() -> {
                            r.run();
                            return null;
                        })),
                Arguments.of("GuiHelper::runInEDTAndWait", (Consumer<Runnable>) GuiHelper::runInEDTAndWait),
                Arguments.of("MainApplication.worker", (Consumer<Runnable>) runnable -> {
                    MainApplication.worker.execute(runnable);
                    assertDoesNotThrow(() -> MainApplication.worker.submit(() -> { /* Sync thread */}).get(1, TimeUnit.SECONDS));
                })
        );
    }

    @ParameterizedTest
    @MethodSource
    void testSuppressedExceptions(String workerName, Consumer<Runnable> worker) {
        // Throw a npe in the worker. Workers might give us the exception, wrapped or otherwise.
        try {
            worker.accept(() -> {
                throw new NullPointerException();
            });
        } catch (Exception e) {
            // pass. MainApplication.worker can continue throwing the NPE;
            Logging.trace(e);
        }
        // Now throw an exception
        BugReport bugReport = new BugReport(BugReport.intercept(new IOException("testSuppressedExceptions")));
        String report = bugReport.getReportText(workerName);
        assertTrue(report.contains("IOException"));
        assertTrue(report.contains("NullPointerException"));
    }

    @Test
    void testSuppressedExceptionsReportedOnce() {
        // Add the exception
        BugReport.addSuppressedException(new NullPointerException("testSuppressedExceptionsReportedOnce"));
        BugReport bugReport = new BugReport(BugReport.intercept(new IOException("testSuppressedExceptionsReportedOnce")));
        // Get the report which clears the suppressed exceptions
        String report = bugReport.getReportText("");
        assertTrue(report.contains("IOException"));
        assertTrue(report.contains("NullPointerException"));

        BugReport bugReport2 = new BugReport(BugReport.intercept(new IOException("testSuppressedExceptionsReportedOnce")));
        String report2 = bugReport2.getReportText("");
        assertTrue(report2.contains("IOException"));
        assertFalse(report2.contains("NullPointerException"));
    }

    @Test
    void testManyExceptions() throws ReflectiveOperationException {
        Field suppressedExceptions = BugReport.class.getDeclaredField("MAXIMUM_SUPPRESSED_EXCEPTIONS");
        ReflectionUtils.makeAccessible(suppressedExceptions);
        final byte expected = suppressedExceptions.getByte(null);
        final int end = 2 * expected;
        // Add many suppressed exceptions
        for (int i = 0; i < end; i++) {
            BugReport.addSuppressedException(new NullPointerException("NPE: " + i));
        }
        BugReport bugReport = new BugReport(BugReport.intercept(new IOException("testManyExceptions")));
        String report = bugReport.getReportText("");
        Matcher matcher = Pattern.compile("NPE: (\\d+)").matcher(report);
        for (int i = end - expected; i < end; ++i) {
            assertTrue(matcher.find());
            assertEquals(Integer.toString(i), matcher.group(1));
        }
        assertFalse(matcher.find());
    }

    @Test
    void testNullException() {
        // This should add a NPE to the suppressed exceptions
        assertDoesNotThrow(() -> BugReport.addSuppressedException(null));
        BugReport bugReport = new BugReport(BugReport.intercept(new IOException("testNullException")));
        // Getting the report text should not throw an exception.
        String report = assertDoesNotThrow(() -> bugReport.getReportText(""));
        assertTrue(report.contains("IOException"));
        assertTrue(report.contains("NullPointerException"));
    }
}
