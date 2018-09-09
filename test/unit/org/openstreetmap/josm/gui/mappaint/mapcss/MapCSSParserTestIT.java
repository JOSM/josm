// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import java.io.IOException;
import java.net.URL;

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
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void testKothicStylesheets() throws IOException {
        new MapCSSParser(new URL("https://raw.githubusercontent.com/kothic/kothic/master/src/styles/default.mapcss").openStream(), "UTF-8");
        new MapCSSParser(new URL("https://raw.githubusercontent.com/kothic/kothic/master/src/styles/mapink.mapcss").openStream(), "UTF-8");
    }
}
