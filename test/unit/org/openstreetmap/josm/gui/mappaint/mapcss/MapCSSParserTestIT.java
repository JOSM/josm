// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.MapCSSParser;
import org.openstreetmap.josm.testutils.annotations.HTTPS;
import org.openstreetmap.josm.testutils.annotations.Projection;

/**
 * Integration tests of {@link MapCSSParser}.
 */
@HTTPS
@Projection
class MapCSSParserTestIT {
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
