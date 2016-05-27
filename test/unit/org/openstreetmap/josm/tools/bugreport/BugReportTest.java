// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.bugreport;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Tests the bug report class.
 * @author Michael Zangl
 * @since 10285
 */
public class BugReportTest {

    /**
     * Test {@link BugReport#getCallingMethod(int)}
     */
    @Test
    public void testGetCallingMethod() {
        assertEquals("BugReportTest#testGetCallingMethod", BugReport.getCallingMethod(1));
        assertEquals("BugReportTest#testGetCallingMethod", testGetCallingMethod2());
    }

    private String testGetCallingMethod2() {
        return BugReport.getCallingMethod(2);
    }
}
