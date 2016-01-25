// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;

/**
 * Unit tests of {@link ChangesetTagsPanel} class.
 */
public class ChangesetTagsPanelTest {

    /**
     * Setup tests
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link ChangesetTagsPanel#ChangesetTagsPanel}.
     */
    @Test
    public void testChangesetTagsPanel() {
        assertNotNull(new ChangesetTagsPanel());
    }
}
