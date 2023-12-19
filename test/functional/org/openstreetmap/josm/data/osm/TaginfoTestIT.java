// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.validation.tests.MapCSSTagChecker;
import org.openstreetmap.josm.data.validation.tests.TagChecker;
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.ParseException;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresets;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.tools.HttpClient;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

/**
 * Various integration tests with Taginfo.
 */
@BasicPreferences
@Timeout(20)
class TaginfoTestIT {
    /**
     * Checks that popular tags are known (i.e included in internal presets, or deprecated, or explicitely ignored)
     * @throws IOException if any I/O error occurs
     * @throws ParseException if any MapCSS parsing error occurs
     */
    @Test
    void testCheckPopularTags() throws IOException, ParseException {
        TaggingPresets.readFromPreferences();
        new TagChecker().initialize();
        MapCSSTagChecker mapCssTagChecker = new MapCSSTagChecker();
        mapCssTagChecker.addMapCSS("resource://data/validator/deprecated.mapcss");

        List<String> errors = new ArrayList<>();
        try (InputStream in = HttpClient.create(new URL("https://taginfo.openstreetmap.org/api/4/tags/popular")).connect().getContent();
             JsonReader reader = Json.createReader(in)) {
            for (JsonValue item : reader.readObject().getJsonArray("data")) {
                JsonObject obj = (JsonObject) item;
                // Only consider tags with wiki pages
                if (obj.getInt("in_wiki") == 1) {
                    String key = obj.getString("key");
                    String value = obj.getString("value");
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
                                ok = !errors.add(key +"="+ value + " - " + obj.getInt("count_all"));
                            }
                        }
                    }
                    System.out.println(ok ? "OK" : "KO");
                }
            }
        }
        for (String error : errors) {
            System.err.println(error);
        }
        assertTrue(errors.isEmpty(), errors::toString);
    }
}
