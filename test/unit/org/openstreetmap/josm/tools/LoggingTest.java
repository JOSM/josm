// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * @author michael
 *
 */
public class LoggingTest {

    private LogRecord captured;
    private final Handler handler = new Handler() {

        @Override
        public void publish(LogRecord record) {
            captured = record;
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {
        }
    };

    /**
     * @throws SecurityException if a security error occurs
     */
    @Before
    public void setUp() throws SecurityException {
        captured = null;
        Logging.getLogger().addHandler(handler);
    }

    /**
     * @throws SecurityException if a security error occurs
     */
    @After
    public void tearDown() throws SecurityException {
        Logging.getLogger().removeHandler(handler);
    }

    /**
     * Test method for {@link org.openstreetmap.josm.tools.Logging#setLogLevel(java.util.logging.Level)}.
     */
    @Test
    public void testSetLogLevel() {
        Logging.setLogLevel(Logging.LEVEL_DEBUG);
        assertEquals(Logging.LEVEL_DEBUG, Logging.getLogger().getLevel());
        Logging.setLogLevel(Logging.LEVEL_WARN);
        assertEquals(Logging.LEVEL_WARN, Logging.getLogger().getLevel());
    }

    private void testLogCaptured(Level level, String expected, Runnable printMessage) {
        testLogCaptured(level, result -> assertEquals(expected, result), printMessage);
    }

    @SuppressFBWarnings(value = "NP_NONNULL_PARAM_VIOLATION")
    private void testLogCaptured(Level level, Consumer<String> expectedTester, Runnable printMessage) {
        Logging.setLogLevel(level);
        captured = null;
        printMessage.run();

        assertNotNull(captured);
        expectedTester.accept(captured.getMessage());
        assertEquals(level, captured.getLevel());

        captured = null;
        Logging.setLogLevel(Level.OFF);
        printMessage.run();
        assertNull(captured);
    }

    /**
     * Test method for {@link org.openstreetmap.josm.tools.Logging#error(java.lang.String)}.
     */
    @Test
    public void testErrorString() {
        testLogCaptured(Logging.LEVEL_ERROR, "test", () -> Logging.error("test"));
    }

    /**
     * Test method for {@link org.openstreetmap.josm.tools.Logging#error(java.lang.String, java.lang.Object[])}.
     */
    @Test
    public void testErrorStringObjectArray() {
        testLogCaptured(Logging.LEVEL_ERROR, "test x 1", () -> Logging.error("test {0} {1}", "x", 1));
    }

    /**
     * Test method for {@link org.openstreetmap.josm.tools.Logging#warn(java.lang.String)}.
     */
    @Test
    public void testWarnString() {
        testLogCaptured(Logging.LEVEL_WARN, "test", () -> Logging.warn("test"));
    }

    /**
     * Test method for {@link org.openstreetmap.josm.tools.Logging#warn(java.lang.String, java.lang.Object[])}.
     */
    @Test
    public void testWarnStringObjectArray() {
        testLogCaptured(Logging.LEVEL_WARN, "test x 1", () -> Logging.warn("test {0} {1}", "x", 1));
    }

    /**
     * Test method for {@link org.openstreetmap.josm.tools.Logging#info(java.lang.String)}.
     */
    @Test
    public void testInfoString() {
        testLogCaptured(Logging.LEVEL_INFO, "test", () -> Logging.info("test"));
    }

    /**
     * Test method for {@link org.openstreetmap.josm.tools.Logging#info(java.lang.String, java.lang.Object[])}.
     */
    @Test
    public void testInfoStringObjectArray() {
        testLogCaptured(Logging.LEVEL_INFO, "test x 1", () -> Logging.info("test {0} {1}", "x", 1));
    }

    /**
     * Test method for {@link org.openstreetmap.josm.tools.Logging#debug(java.lang.String)}.
     */
    @Test
    public void testDebugString() {
        testLogCaptured(Logging.LEVEL_DEBUG, "test", () -> Logging.debug("test"));
    }

    /**
     * Test method for {@link org.openstreetmap.josm.tools.Logging#debug(java.lang.String, java.lang.Object[])}.
     */
    @Test
    public void testDebugStringObjectArray() {
        testLogCaptured(Logging.LEVEL_DEBUG, "test x 1", () -> Logging.debug("test {0} {1}", "x", 1));
    }

    /**
     * Test method for {@link org.openstreetmap.josm.tools.Logging#trace(java.lang.String)}.
     */
    @Test
    public void testTraceString() {
        testLogCaptured(Logging.LEVEL_TRACE, "test", () -> Logging.trace("test"));
    }

    /**
     * Test method for {@link org.openstreetmap.josm.tools.Logging#trace(java.lang.String, java.lang.Object[])}.
     */
    @Test
    public void testTraceStringObjectArray() {
        testLogCaptured(Logging.LEVEL_TRACE, "test x 1", () -> Logging.trace("test {0} {1}", "x", 1));
    }

    /**
     * Test method for {@link org.openstreetmap.josm.tools.Logging#log(java.util.logging.Level, java.lang.Throwable)}.
     */
    @Test
    public void testLogLevelThrowable() {
        testLogCaptured(Logging.LEVEL_ERROR, "java.io.IOException: x", () -> Logging.log(Logging.LEVEL_ERROR, new IOException("x")));

        testLogCaptured(Logging.LEVEL_TRACE, "java.io.IOException: x", () -> Logging.log(Logging.LEVEL_TRACE, new IOException("x")));
    }

    /**
     * Test method for {@link org.openstreetmap.josm.tools.Logging#log(java.util.logging.Level, java.lang.String, java.lang.Throwable)}.
     */
    @Test
    public void testLogLevelStringThrowable() {
        testLogCaptured(Logging.LEVEL_ERROR, "y: java.io.IOException: x", () -> Logging.log(Logging.LEVEL_ERROR, "y", new IOException("x")));

        testLogCaptured(Logging.LEVEL_TRACE, "y: java.io.IOException: x", () -> Logging.log(Logging.LEVEL_TRACE, "y", new IOException("x")));
    }

    /**
     * Test method for {@link org.openstreetmap.josm.tools.Logging#logWithStackTrace(java.util.logging.Level, java.lang.Throwable)}.
     */
    @Test
    public void testLogWithStackTraceLevelThrowable() {
        Consumer<String> test = string -> {
            assertTrue(string.startsWith("java.io.IOException: x"));
            assertTrue(string.contains("testLogWithStackTraceLevelThrowable"));
        };
        testLogCaptured(Logging.LEVEL_ERROR, test, () -> Logging.logWithStackTrace(Logging.LEVEL_ERROR, new IOException("x")));
        testLogCaptured(Logging.LEVEL_TRACE, test, () -> Logging.logWithStackTrace(Logging.LEVEL_TRACE, new IOException("x")));

        testLogCaptured(Logging.LEVEL_TRACE, string -> assertTrue(string.startsWith("java.io.IOException\n")),
                () -> Logging.logWithStackTrace(Logging.LEVEL_TRACE, new IOException()));

        testLogCaptured(Logging.LEVEL_TRACE, string -> assertTrue(string.contains("Cause:")),
                () -> Logging.logWithStackTrace(Logging.LEVEL_TRACE, new IOException(new IOException())));

    }

    /**
     * Test method for {@link Logging#logWithStackTrace(Level, String, Throwable)}.
     */
    @Test
    public void testLogWithStackTraceLevelStringThrowable() {
        Consumer<String> test = string -> {
            assertTrue(string.startsWith("y: java.io.IOException: x"));
            assertTrue(string.indexOf("testLogWithStackTraceLevelStringThrowable") > 0);
        };
        testLogCaptured(Logging.LEVEL_ERROR, test, () -> Logging.logWithStackTrace(Logging.LEVEL_ERROR, "y", new IOException("x")));
        testLogCaptured(Logging.LEVEL_TRACE, test, () -> Logging.logWithStackTrace(Logging.LEVEL_TRACE, "y", new IOException("x")));
    }

    /**
     * Test method for {@link org.openstreetmap.josm.tools.Logging#isLoggingEnabled(java.util.logging.Level)}.
     */
    @Test
    public void testIsLoggingEnabled() {
        Logging.setLogLevel(Logging.LEVEL_ERROR);
        assertTrue(Logging.isLoggingEnabled(Logging.LEVEL_ERROR));
        assertFalse(Logging.isLoggingEnabled(Logging.LEVEL_INFO));
        assertFalse(Logging.isLoggingEnabled(Logging.LEVEL_TRACE));
        Logging.setLogLevel(Logging.LEVEL_INFO);
        assertTrue(Logging.isLoggingEnabled(Logging.LEVEL_ERROR));
        assertTrue(Logging.isLoggingEnabled(Logging.LEVEL_INFO));
        assertFalse(Logging.isLoggingEnabled(Logging.LEVEL_TRACE));
        Logging.setLogLevel(Logging.LEVEL_TRACE);
        assertTrue(Logging.isLoggingEnabled(Logging.LEVEL_ERROR));
        assertTrue(Logging.isLoggingEnabled(Logging.LEVEL_INFO));
        assertTrue(Logging.isLoggingEnabled(Logging.LEVEL_TRACE));
    }

    /**
     * Test method for {@link org.openstreetmap.josm.tools.Logging#clearLastErrorAndWarnings()}.
     */
    @Test
    public void testClearLastErrorAndWarnings() {
        Logging.setLogLevel(Logging.LEVEL_WARN);
        Logging.clearLastErrorAndWarnings();
        Logging.error("x");
        assertFalse(Logging.getLastErrorAndWarnings().isEmpty());
        assertFalse(Logging.getLastErrorAndWarnings().isEmpty());
        Logging.clearLastErrorAndWarnings();
        assertTrue(Logging.getLastErrorAndWarnings().isEmpty());
    }

    /**
     * Test method for {@link org.openstreetmap.josm.tools.Logging#getLastErrorAndWarnings()}.
     */
    @Test
    public void testGetLastErrorAndWarnings() {
        Logging.setLogLevel(Logging.LEVEL_WARN);
        Logging.clearLastErrorAndWarnings();
        Logging.warn("x");

        assertEquals(1, Logging.getLastErrorAndWarnings().size());
        assertTrue(Logging.getLastErrorAndWarnings().toString(), Logging.getLastErrorAndWarnings().get(0).endsWith("W: x"));

        Logging.setLogLevel(Logging.LEVEL_ERROR);
        Logging.warn("x");

        assertEquals(1, Logging.getLastErrorAndWarnings().size());

        Logging.error("y\nz");

        assertEquals(2, Logging.getLastErrorAndWarnings().size());
        assertTrue(Logging.getLastErrorAndWarnings().toString(), Logging.getLastErrorAndWarnings().get(0).endsWith("W: x"));
        assertTrue(Logging.getLastErrorAndWarnings().toString(), Logging.getLastErrorAndWarnings().get(1).endsWith("E: y"));

        // limit somewhere reasonable
        for (int i = 3; i < 6; i++) {
            Logging.error("x");
            assertEquals(i, Logging.getLastErrorAndWarnings().size());
        }
        for (int i = 2; i < 100; i++) {
            Logging.error("x");
        }
        assertTrue(Logging.getLastErrorAndWarnings().size() < 101);
    }
}
