// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset.query;

import static org.junit.Assert.assertNotNull;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link AdvancedChangesetQueryPanel} class.
 */
public class AdvancedChangesetQueryPanelTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Unit test of {@link AdvancedChangesetQueryPanel#AdvancedChangesetQueryPanel}.
     */
    @Test
    public void testAdvancedChangesetQueryPanel() {
        assertNotNull(new AdvancedChangesetQueryPanel());
    }
}
