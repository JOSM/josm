// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.gui.preferences.server.OverpassServerPreference;
import org.openstreetmap.josm.io.OverpassDownloadReader.OverpassOutpoutFormat;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.OverpassTurboQueryWizard;
import org.openstreetmap.josm.tools.Utils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link OverpassDownloadReader} class.
 */
public class OverpassDownloadReaderTest {

    /**
     * Base test environment is enough
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().timeout(15000);

    private String getExpandedQuery(String search) {
        final String query = OverpassTurboQueryWizard.getInstance().constructQuery(search);
        final String request = new OverpassDownloadReader(new Bounds(1, 2, 3, 4), OverpassServerPreference.getOverpassServer(), query)
                .getRequestForBbox(1, 2, 3, 4)
                .substring("interpreter?data=".length());
        return Utils.decodeUrl(request);
    }

    /**
     * Tests evaluating the extended query feature {@code bbox}.
     */
    @Test
    public void testBbox() {
        final String query = getExpandedQuery("amenity=drinking_water");
        assertEquals("" +
                "[out:xml][timeout:25][bbox:2.0,1.0,4.0,3.0];\n" +
                "(\n" +
                "  node[\"amenity\"=\"drinking_water\"];\n" +
                "  way[\"amenity\"=\"drinking_water\"];\n" +
                "  relation[\"amenity\"=\"drinking_water\"];\n" +
                ");\n" +
                "(._;>;);\n" +
                "out meta;", query);
    }

    /**
     * Tests evaluating the extended query feature {@code geocodeArea}.
     */
    @Test
    public void testGeocodeArea() {
        final String query = getExpandedQuery("amenity=drinking_water in London");
        assertEquals("" +
                "[out:xml][timeout:25];\n" +
                "area(3600065606)->.searchArea;\n" +
                "(\n" +
                "  node[\"amenity\"=\"drinking_water\"](area.searchArea);\n" +
                "  way[\"amenity\"=\"drinking_water\"](area.searchArea);\n" +
                "  relation[\"amenity\"=\"drinking_water\"](area.searchArea);\n" +
                ");\n" +
                "(._;>;);\n" +
                "out meta;", query);
    }

    /**
     * Tests evaluating the extended query feature {@code geocodeArea}.
     */
    @Test
    public void testGeocodeUnknownArea() {
        final String query = OverpassDownloadReader.expandExtendedQueries("{{geocodeArea:foo-bar-baz-does-not-exist}}");
        assertEquals("// Failed to evaluate {{geocodeArea:foo-bar-baz-does-not-exist}}\n", query);
    }

    /**
     * Tests evaluating the overpass output format statements.
     */
    @Test
    public void testOutputFormatStatement() {
        for (OverpassOutpoutFormat oof : OverpassOutpoutFormat.values()) {
            assertTrue(OverpassDownloadReader.OUTPUT_FORMAT_STATEMENT.matcher("[out:"+oof.getDirective()+"]").matches());
        }

        assertTrue(OverpassDownloadReader.OUTPUT_FORMAT_STATEMENT.matcher(
                "[out:pbf][timeout:25][bbox:{{bbox}}];\n" +
                "(\n" +
                "  node[\"amenity\"=\"pharmacy\"];\n" +
                "  way[\"amenity\"=\"pharmacy\"];\n" +
                "  relation[\"amenity\"=\"pharmacy\"];\n" +
                ");\n" +
                "(._;>;);\n" +
                "out meta;").matches());
    }
}
