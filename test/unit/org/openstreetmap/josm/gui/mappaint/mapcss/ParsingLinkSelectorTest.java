// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.annotations.Projection;

/**
 * Unit tests of {@code ParsingLinkSelector}.
 */
@Projection
class ParsingLinkSelectorTest {
    @Test
    void testParseEmptyChildSelector() {
        String css = "relation > way {}";
        MapCSSStyleSource source = new MapCSSStyleSource(css);
        source.loadStyleSource();
        assertEquals(1, source.rules.size());
    }

    @Test
    void testParseEmptyParentSelector() {
        String css = "way < relation {}";
        MapCSSStyleSource source = new MapCSSStyleSource(css);
        source.loadStyleSource();
        assertEquals(1, source.rules.size());
    }

    @Test
    void testParseChildSelectorWithKeyValueCondition() {
        String css = "relation >[role=\"my_role\"] way {}";
        MapCSSStyleSource source = new MapCSSStyleSource(css);
        source.loadStyleSource();
        assertEquals(1, source.rules.size());
    }

    @Test
    void testParseChildSelectorWithKeyCondition() {
        String css = "relation >[\"my_role\"] way{}";
        MapCSSStyleSource source = new MapCSSStyleSource(css);
        source.loadStyleSource();
        assertEquals(1, source.rules.size());
    }
}
