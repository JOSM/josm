// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.annotations.Main;

/**
 * Unit tests of {@link ChangesetContentPanel} class.
 */
@Main
class ChangesetContentPanelTest {
    /**
     * Unit test of {@link ChangesetContentPanel#ChangesetContentPanel}.
     */
    @Test
    void testChangesetContentPanel() {
        assertNotNull(new ChangesetContentPanel());
    }
}

