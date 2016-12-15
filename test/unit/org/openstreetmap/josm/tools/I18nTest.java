// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests of {@link I18n}.
 */
public class I18nTest {

    /**
     * Unit test of {@link I18n#escape}.
     */
    @Test
    public void testEscape() {
        String foobar = "{foo'bar}";
        assertEquals("'{'foo''bar'}'", I18n.escape(foobar));
        assertEquals(foobar, I18n.tr(I18n.escape(foobar)));
    }
}
