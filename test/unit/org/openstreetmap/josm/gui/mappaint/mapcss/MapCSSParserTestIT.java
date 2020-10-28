// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.MapCSSParser;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Integration tests of {@link MapCSSParser}.
 */
class MapCSSParserTestIT {

    /**
     * Setup rule
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().https().projection();

    /**
     * Checks Kothic stylesheets
     */
    @Test
    @Disabled("parsing fails")
    void testKothicStylesheets() {
        new MapCSSStyleSource("https://raw.githubusercontent.com/kothic/kothic/master/src/styles/default.mapcss").loadStyleSource();
        new MapCSSStyleSource("https://raw.githubusercontent.com/kothic/kothic/master/src/styles/mapink.mapcss").loadStyleSource();
    }
}
