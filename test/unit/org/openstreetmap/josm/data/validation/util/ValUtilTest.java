// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.util;

import org.junit.jupiter.api.Test;

import net.trajano.commons.testing.UtilityClassTestUtil;

/**
 * Unit tests for class {@link ValUtil}.
 */
class ValUtilTest {
    /**
     * Tests that {@code ValUtil} satisfies utility class criteria.
     * @throws ReflectiveOperationException if an error occurs
     */
    @Test
    void testUtilityClass() throws ReflectiveOperationException {
        UtilityClassTestUtil.assertUtilityClassWellDefined(ValUtil.class);
    }
}
