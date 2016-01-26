// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;

/**
 * Unit tests of {@link ChangesetDiscussionPanel} class.
 */
public class ChangesetDiscussionPanelTest {

    /**
     * Setup tests
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link ChangesetDiscussionPanel#ChangesetDiscussionPanel}.
     */
    @Test
    public void testChangesetDiscussionPanel() {
        assertNotNull(new ChangesetDiscussionPanel());
    }
}
