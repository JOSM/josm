// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.MapCSSParser;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Integration tests of {@link MapCSSParser}.
 */
public class MapCSSParserTestIT {

    /**
     * Setup rule
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().https().projection();

    /**
     * Checks Kothic stylesheets
     */
    @Test
    @Ignore("parsing fails")
    public void testKothicStylesheets() {
        new MapCSSStyleSource("https://raw.githubusercontent.com/kothic/kothic/master/src/styles/default.mapcss").loadStyleSource();
        new MapCSSStyleSource("https://raw.githubusercontent.com/kothic/kothic/master/src/styles/mapink.mapcss").loadStyleSource();
    }
}
