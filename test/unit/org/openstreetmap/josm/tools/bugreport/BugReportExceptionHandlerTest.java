// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.bugreport;

import static org.junit.Assert.assertNotNull;

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
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test for {@link BugReportExceptionHandler#buildPanel} method.
     */
    @Test
    public void testBuildPanel() {
        assertNotNull(BugReportExceptionHandler.buildPanel(new Exception()));
    }
}
