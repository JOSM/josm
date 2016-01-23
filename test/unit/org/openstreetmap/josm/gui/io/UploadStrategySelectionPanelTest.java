// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;

/**
 * Unit tests of {@link UploadStrategySelectionPanel} class.
 */
public class UploadStrategySelectionPanelTest {

    /**
     * Setup tests
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Test of {@link UploadStrategySelectionPanel#UploadStrategySelectionPanel}.
     */
    @Test
    public void testUploadStrategySelectionPanel() {
        assertNotNull(new UploadStrategySelectionPanel());
    }
}
