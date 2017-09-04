// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.junit.Assert.assertNotNull;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link TagSettingsPanel} class.
 */
public class TagSettingsPanelTest {

    /**
     * Setup tests
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Test of {@link TagSettingsPanel#TagSettingsPanel}.
     */
    @Test
    public void testTagSettingsPanel() {
        assertNotNull(new TagSettingsPanel(new ChangesetCommentModel(), new ChangesetCommentModel(), new ChangesetReviewModel()));
    }
}
