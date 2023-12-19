// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.annotations.Main;

/**
 * Unit tests for class {@link SessionSaveAsAction}.
 */
@Main
class SessionSaveAsActionTest {
    /**
     * Unit test of {@link SessionSaveAsAction#actionPerformed}
     */
    @Test
    void testSessionSaveAsAction() {
        SessionSaveAsAction action = new SessionSaveAsAction();
        assertFalse(action.isEnabled());
        action.actionPerformed(null);
    }
}
