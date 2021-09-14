// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

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
        assertDoesNotThrow(() -> action.actionPerformed(null));
        assertDoesNotThrow(() -> action.actionPerformed(null));
    }
}

