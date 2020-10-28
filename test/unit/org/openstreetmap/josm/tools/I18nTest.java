// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link I18n}.
 */
class I18nTest {

    /**
     * Unit test of {@link I18n#escape}.
     */
    @Test
    void testEscape() {
        String foobar = "{foo'bar}";
        assertEquals("'{'foo''bar'}'", I18n.escape(foobar));
        assertEquals(foobar, I18n.tr(I18n.escape(foobar)));
    }
}
