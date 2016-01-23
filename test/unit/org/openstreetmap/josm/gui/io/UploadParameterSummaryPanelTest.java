// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;

/**
 * Unit tests of {@link UploadParameterSummaryPanel} class.
 */
public class UploadParameterSummaryPanelTest {

    /**
     * Setup tests
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Test of {@link UploadParameterSummaryPanel#UploadParameterSummaryPanel}.
     */
    @Test
    public void testUploadParameterSummaryPanel() {
        assertNotNull(new UploadParameterSummaryPanel());
    }
}
