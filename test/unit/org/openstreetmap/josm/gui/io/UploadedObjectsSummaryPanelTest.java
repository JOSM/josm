// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.junit.Assert.assertNotNull;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link UploadedObjectsSummaryPanel} class.
 */
public class UploadedObjectsSummaryPanelTest {

    /**
     * Setup tests
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Test of {@link UploadedObjectsSummaryPanel#UploadedObjectsSummaryPanel}.
     */
    @Test
    public void testUploadedObjectsSummaryPanel() {
        assertNotNull(new UploadedObjectsSummaryPanel());
    }
}
