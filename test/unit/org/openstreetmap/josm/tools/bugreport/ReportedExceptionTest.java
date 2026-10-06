// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.bugreport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.InvalidPathException;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

/**
 * Tests the {@link ReportedException} class.
 * @author Michael Zangl
 * @since 10285
 */
class ReportedExceptionTest {
    private static final class CauseOverwriteException extends RuntimeException {
        private Throwable myCause;

        private CauseOverwriteException(String message) {
            super(message);
        }

        @Override
        public synchronized Throwable getCause() {
            return myCause;
        }
    }

    /**
     * Tests that {@link ReportedException#put(String, Object)} handles null values
     */
    @Test
    void testPutDoesHandleNull() {
        ReportedException e = new ReportedException(new RuntimeException());
        e.startSection("test");
        Object[] a = new Object[] {
                new Object(), null };
        e.put("testObject", null);
        e.put("testArray", a);
        e.put("testList", Arrays.asList(a));
    }

    /**
     * Tests that {@link ReportedException#put(String, Object)} handles exceptions during toString fine.
     */
    @Test
    void testPutDoesNotThrow() {
        ReportedException e = new ReportedException(new RuntimeException());
        e.startSection("test");
        Object o = new Object() {
            @Override
            public String toString() {
                throw new IllegalArgumentException("");
            }
        };
        Object[] a = new Object[] {
                new Object(), o };
        e.put("testObject", o);
        e.put("testArray", a);
        e.put("testList", Arrays.asList(a));
    }

    /**
     * Tests {@link ReportedException#isUnmappableFileName()}: a file name which the character set used for file names
     * cannot represent is recognised, also as the cause of another exception, but only if that character set is not
     * UTF-8 - otherwise the locale is not to blame. See #14596.
     */
    @Test
    void testIsUnmappableFileName() {
        // the exception sun.nio.fs.UnixPath throws for "Capture d'écran.png" when the locale is C or not installed
        InvalidPathException unmappable = new InvalidPathException("/home/user/Capture d'\u00e9cran.png",
                "Malformed input or input contains unmappable characters");
        String jnuEncoding = System.getProperty("sun.jnu.encoding");
        try {
            System.setProperty("sun.jnu.encoding", "ANSI_X3.4-1968");
            assertTrue(new ReportedException(unmappable).isUnmappableFileName());
            assertTrue(new ReportedException(new IllegalStateException(unmappable)).isUnmappableFileName());
            assertFalse(new ReportedException(new InvalidPathException("a\0b", "Nul character not allowed")).isUnmappableFileName());
            assertFalse(new ReportedException(new IllegalStateException()).isUnmappableFileName());

            System.setProperty("sun.jnu.encoding", "UTF-8");
            assertFalse(new ReportedException(unmappable).isUnmappableFileName());
        } finally {
            System.setProperty("sun.jnu.encoding", jnuEncoding);
        }
    }

    /**
     * Tests that {@link ReportedException#isSame(ReportedException)} works as expected.
     */
    @Test
    void testIsSame() {
        // Do not break this line! All exceptions need to be created in the same line.
        // CHECKSTYLE.OFF: LineLength
        // @formatter:off
        ReportedException[] testExceptions = new ReportedException[] {
                /* 0 */ genException1(), /* 1, same as 0 */ genException1(), /* 2 */ genException2("x"), /* 3, same as 2 */ genException2("x"), /* 4, has different message than 2 */ genException2("y"), /* 5, has different stack trace than 2 */ genException3("x"), /* 6 */ genException4(true), /* 7, has different cause than 6 */ genException4(false), /* 8, has a cycle and should not crash */ genExceptionCycle() };
        // @formatter:on
        // CHECKSTYLE.ON: LineLength

        for (int i = 0; i < testExceptions.length; i++) {
            for (int j = 0; j < testExceptions.length; j++) {
                boolean is01 = (i == 0 || i == 1) && (j == 0 || j == 1);
                boolean is23 = (i == 2 || i == 3) && (j == 2 || j == 3);
                assertEquals(is01 || is23 || i == j, testExceptions[i].isSame(testExceptions[j]), i + ", " + j);
            }
        }
    }

    private static ReportedException genException1() {
        RuntimeException e = new RuntimeException();
        return BugReport.intercept(e);
    }

    private static ReportedException genException2(String message) {
        RuntimeException e = new RuntimeException(message);
        RuntimeException e2 = new RuntimeException(e);
        return BugReport.intercept(e2);
    }

    private static ReportedException genException3(String message) {
        return genException2(message);
    }

    private static ReportedException genException4(boolean addCause) {
        RuntimeException e = new RuntimeException("x");
        RuntimeException e2 = new RuntimeException("x", addCause ? e : null);
        return BugReport.intercept(e2);
    }

    private static ReportedException genExceptionCycle() {
        CauseOverwriteException e = new CauseOverwriteException("x");
        RuntimeException e2 = new RuntimeException("x", e);
        e.myCause = e2;
        return BugReport.intercept(e2);
    }
}
