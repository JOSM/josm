// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests of {@code ParsingLinkSelector}.
 */
@BasicPreferences
class ParsingLinkSelectorTest {
    @Test
    public void parseEmptyChildSelector() {
        String css = "relation > way {}";
        MapCSSStyleSource source = new MapCSSStyleSource(css);
        source.loadStyleSource();
        assertEquals(1, source.rules.size());
    }

    @Test
    public void parseEmptyParentSelector() {
        String css = "way < relation {}";
        MapCSSStyleSource source = new MapCSSStyleSource(css);
        source.loadStyleSource();
        assertEquals(1, source.rules.size());
    }

    @Test
    public void parseChildSelectorWithKeyValueCondition() {
        String css = "relation >[role=\"my_role\"] way {}";
        MapCSSStyleSource source = new MapCSSStyleSource(css);
        source.loadStyleSource();
        assertEquals(1, source.rules.size());
    }

    @Test
    public void parseChildSelectorWithKeyCondition() {
        String css = "relation >[\"my_role\"] way{}";
        MapCSSStyleSource source = new MapCSSStyleSource(css);
        source.loadStyleSource();
        assertEquals(1, source.rules.size());
    }
}
