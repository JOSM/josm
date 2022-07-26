// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.validation.tests.MapCSSTagChecker;
import org.openstreetmap.josm.data.validation.tests.TagChecker;
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.ParseException;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.HTTP;
import org.openstreetmap.josm.testutils.annotations.IntegrationTest;
import org.openstreetmap.josm.testutils.annotations.Presets;
import org.openstreetmap.josm.tools.HttpClient;

/**
 * Various integration tests with Taginfo.
 */
@Timeout(value = 20)
@BasicPreferences
@HTTP
@Presets
@IntegrationTest
class TaginfoTestIT {

    private static MapCSSTagChecker mapCssTagChecker;

    /**
     * Generate the tags to check
     * @return A stream of key, value arguments
     * @throws IOException if any I/O error occurs
     */
    static Stream<Arguments> testCheckPopularTags() throws IOException {
        final Stream.Builder<Arguments> builder = Stream.builder();
        try (InputStream in = HttpClient.create(new URL("https://taginfo.openstreetmap.org/api/4/tags/popular")).connect().getContent();
                JsonReader reader = Json.createReader(in)) {
            for (JsonValue item : reader.readObject().getJsonArray("data")) {
                JsonObject obj = (JsonObject) item;
                // Only consider tags with wiki pages
                if (obj.getInt("in_wiki") == 1) {
                    String key = obj.getString("key");
                    String value = obj.getString("value");
                    builder.add(Arguments.of(key, value, obj.getInt("count_all")));
                }
            }
        }
        return builder.build();
    }

    /**
     * Set up common test requirements
     *
     * @throws IOException if any I/O error occurs
     * @throws ParseException if any MapCSS parsing error occurs
     */
    @BeforeAll
    static void setUp() throws IOException, ParseException {
        new TagChecker().initialize();
        mapCssTagChecker = new MapCSSTagChecker();
        mapCssTagChecker.addMapCSS("resource://data/validator/deprecated.mapcss");
    }

    /**
     * Checks that popular tags are known (i.e included in internal presets, or deprecated, or explicitely ignored)
     */
    @ParameterizedTest
    @MethodSource
    void testCheckPopularTags(final String key, final String value, final int countAll) {
        System.out.print("Checking "+key+"="+value+" ... ");
        boolean ok = true;
        // Check if tag is in internal presets
        if (!TagChecker.isTagInPresets(key, value)) {
            // If not, check if we have either a deprecated mapcss test for it
            Node n = new Node(LatLon.NORTH_POLE);
            Way w = new Way();
            Relation r = new Relation();
            n.put(key, value);
            w.put(key, value);
            r.put(key, value);
            new DataSet(n, w, r);
            if (mapCssTagChecker.getErrorsForPrimitive(n, false).isEmpty()
             && mapCssTagChecker.getErrorsForPrimitive(w, false).isEmpty()
             && mapCssTagChecker.getErrorsForPrimitive(r, false).isEmpty()) {
                // Or a legacy tagchecker ignore rule
                if (!TagChecker.isTagIgnored(key, value)) {
                    ok = false;
                }
            }
        }
        System.out.println(ok ? "OK" : "KO");
        assertTrue(ok, key + "=" + value + " - " + countAll);
    }
}
