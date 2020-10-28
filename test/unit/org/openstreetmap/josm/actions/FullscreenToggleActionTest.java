// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Test {@link FullscreenToggleAction}
 */
class FullscreenToggleActionTest {
    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().main();

    /**
     * Test {@link FullscreenToggleAction}
     */
    @Test
    void testFullscreenToggleAction() {
        FullscreenToggleAction action = new FullscreenToggleAction();
        // Cannot really test it in headless mode, but at least check we can toggle the action without error
        action.actionPerformed(null);
        action.actionPerformed(null);
    }
}
