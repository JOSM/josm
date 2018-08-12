// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.bugreport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.actions.ShowStatusReportAction;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Tests the bug report class.
 * @author Michael Zangl
 */
public class BugReportTest {
    /**
     * Preferences for the report text
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Test {@link BugReport#getReportText}
     */
    @Test
    public void testReportText() {
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
    public void testIntercept() {
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
    public void testGetCallingMethod() {
        assertEquals("BugReportTest#testGetCallingMethod", BugReport.getCallingMethod(1));
        assertEquals("BugReportTest#testGetCallingMethod", testGetCallingMethod2());
        assertEquals("?", BugReport.getCallingMethod(100));
    }

    private String testGetCallingMethod2() {
        return BugReport.getCallingMethod(2);
    }
}
