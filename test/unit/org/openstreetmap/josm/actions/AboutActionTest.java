// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.annotations.Main;


/**
 * Unit tests for class {@link AboutAction}.
 */
@Main
final class AboutActionTest {
    /**
     * Unit test of {@link AboutAction#buildAboutPanel}.
     */
    @Test
    void testBuildAboutPanel() {
        assertDoesNotThrow(() -> new AboutAction().buildAboutPanel());
    }
}
