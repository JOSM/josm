// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.annotations.Main;

/**
 * Test {@link FullscreenToggleAction}
 */
@Main
class FullscreenToggleActionTest {
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
