// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.MapCSSParser;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.HTTP;
import org.openstreetmap.josm.testutils.annotations.HTTPS;
import org.openstreetmap.josm.testutils.annotations.IntegrationTest;

/**
 * Integration tests of {@link MapCSSParser}.
 */
@IntegrationTest
@BasicPreferences
@HTTP
@HTTPS
class MapCSSParserTestIT {

    /**
     * Checks Kothic stylesheets
     */
    @ParameterizedTest
    @ValueSource(strings = {"default.mapcss", "mapink.mapcss"})
    void testKothicStylesheets(final String styleFile) {
        final MapCSSStyleSource style = new MapCSSStyleSource("https://raw.githubusercontent.com/kothic/kothic/master/src/styles/" + styleFile);
        assertDoesNotThrow((Executable) style::loadStyleSource);
    }
}
