// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset.query;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link UrlBasedQueryPanel} class.
 */
public class UrlBasedQueryPanelTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Unit test of {@link UrlBasedQueryPanel#UrlBasedQueryPanel}.
     */
    @Test
    public void testUrlBasedQueryPanel() {
        assertNotNull(new UrlBasedQueryPanel());
    }

    /**
     * Checks that examples displayed in panel are correct.
     */
    @Test
    public void testExamplesAreCorrect() {
        for (String example : UrlBasedQueryPanel.getExamples()) {
            assertTrue(example, UrlBasedQueryPanel.isValidChangesetQueryUrl(example));
        }
    }
}
