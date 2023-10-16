// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.Main;

/**
 * Unit tests of {@link ChangesetContentPanel} class.
 */
@BasicPreferences
@Main
class ChangesetContentPanelTest {
    /**
     * Unit test of {@link ChangesetContentPanel#ChangesetContentPanel}.
     */
    @Test
    void testChangesetContentPanel() {
        assertDoesNotThrow(ChangesetContentPanel::new);
    }
}
