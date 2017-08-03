// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Test {@link FullscreenToggleAction}
 */
public class FullscreenToggleActionTest {
    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().platform().main();

    /**
     * Test {@link FullscreenToggleAction}
     */
    @Test
    public void testFullscreenToggleAction() {
        FullscreenToggleAction action = new FullscreenToggleAction();
        // Cannot really test it in headless mode, but at least check we can toggle the action without error
        action.actionPerformed(null);
        action.actionPerformed(null);
    }
}
