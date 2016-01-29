// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;

/**
 * Unit tests of {@link ChangesetManagementPanel} class.
 */
public class ChangesetManagementPanelTest {

    /**
     * Setup tests
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Test of {@link ChangesetManagementPanel#ChangesetManagementPanel}.
     */
    @Test
    public void testChangesetManagementPanel() {
        assertNotNull(new ChangesetManagementPanel(new ChangesetCommentModel()));
    }
}
