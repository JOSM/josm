// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import org.junit.jupiter.api.Test;

import net.trajano.commons.testing.UtilityClassTestUtil;

/**
 * Unit tests of {@link ConditionalOptionPaneUtil} class.
 */
class ConditionalOptionPaneUtilTest {
    /**
     * Tests that {@code ConditionalOptionPaneUtil} satisfies utility class criteria.
     * @throws ReflectiveOperationException if an error occurs
     */
    @Test
    void testUtilityClass() throws ReflectiveOperationException {
        UtilityClassTestUtil.assertUtilityClassWellDefined(ConditionalOptionPaneUtil.class);
    }
}
