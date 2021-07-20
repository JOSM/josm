// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.MapCSSParser;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.testutils.annotations.IntegrationTest;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Integration tests of {@link MapCSSParser}.
 */
@IntegrationTest
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
    @ParameterizedTest
    @ValueSource(strings = {"default.mapcss", "mapink.mapcss"})
    void testKothicStylesheets(final String styleFile) {
        final MapCSSStyleSource style = new MapCSSStyleSource("https://raw.githubusercontent.com/kothic/kothic/master/src/styles/" + styleFile);
        assertDoesNotThrow((Executable) style::loadStyleSource);
    }
}
