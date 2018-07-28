// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@code ParsingLinkSelector}.
 */
public class ParsingLinkSelectorTest {

    /**
     * Setup rule
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().projection();

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
