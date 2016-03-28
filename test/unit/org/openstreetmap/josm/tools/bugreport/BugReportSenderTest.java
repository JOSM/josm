// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.bugreport;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.actions.ShowStatusReportAction;

/**
 * Unit tests of {@link BugReportSender} class.
 */
public class BugReportSenderTest {

    /**
     * Setup tests.
     */
    @Before
    public void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test for {@link BugReportSender#BugReportSender}.
     * @throws InterruptedException if the thread is interrupted
     */
    @Test
    public void testBugReportSender() throws InterruptedException {
        BugReportSender sender = BugReportSender.reportBug(ShowStatusReportAction.getReportHeader());
        assertNotNull(sender);
        synchronized (sender) {
            while (sender.isAlive()) {
                sender.wait();
            }
        }
        assertFalse(sender.isAlive());
        assertNull(sender.getErrorMessage(), sender.getErrorMessage());
    }
}
