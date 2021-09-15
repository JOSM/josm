// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.gui.dialogs.ChangesetDialog.LaunchChangesetManager;

import net.trajano.commons.testing.UtilityClassTestUtil;

/**
 * Unit tests of {@link ChangesetDialog} class.
 */
class ChangesetDialogTest {
    /**
     * Tests that {@code LaunchChangesetManager} satisfies utility class criteria.
     * @throws ReflectiveOperationException if an error occurs
     */
    @Test
    void testUtilityClass() throws ReflectiveOperationException {
        UtilityClassTestUtil.assertUtilityClassWellDefined(LaunchChangesetManager.class);
    }
}
