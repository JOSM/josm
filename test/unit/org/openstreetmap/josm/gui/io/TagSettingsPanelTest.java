// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;

/**
 * Unit tests of {@link TagSettingsPanel} class.
 */
public class TagSettingsPanelTest {

    /**
     * Setup tests
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Test of {@link TagSettingsPanel#TagSettingsPanel}.
     */
    @Test
    public void testTagSettingsPanel() {
        assertNotNull(new TagSettingsPanel(new ChangesetCommentModel(), new ChangesetCommentModel()));
    }
}
