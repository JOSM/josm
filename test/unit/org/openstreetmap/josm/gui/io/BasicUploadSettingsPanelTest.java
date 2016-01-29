// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;

/**
 * Unit tests of {@link BasicUploadSettingsPanel} class.
 */
public class BasicUploadSettingsPanelTest {

    /**
     * Setup tests
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Test of {@link BasicUploadSettingsPanel#BasicUploadSettingsPanel}.
     */
    @Test
    public void testBasicUploadSettingsPanel() {
        assertNotNull(new BasicUploadSettingsPanel(new ChangesetCommentModel(), new ChangesetCommentModel()));
    }
}
