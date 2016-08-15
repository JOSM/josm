// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.bugreport;

import static org.junit.Assert.assertFalse;

import org.junit.Before;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;

/**
 * Unit tests of {@link BugReportExceptionHandler} class.
 */
public class BugReportExceptionHandlerTest {

    /**
     * Setup tests.
     */
    @Before
    public void setUp() {
        JOSMFixture.createUnitTestFixture().init(true);
    }

    /**
     * Unit test for {@link BugReportExceptionHandler#handleException} method.
     */
    @Test
    public void testHandleException() {
        BugReportExceptionHandler.handleException(new Exception("testHandleException"));
        assertFalse(BugReportExceptionHandler.exceptionHandlingInProgress());
    }
}
