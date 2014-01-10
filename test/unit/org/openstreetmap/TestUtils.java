// License: GPL. For details, see LICENSE file.
package org.openstreetmap;

import org.junit.Before;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.TextTagParser;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TestUtils {

    /**
     * Returns the path to test data root directory.
     */
    public static String getTestDataRoot() {
        String testDataRoot = System.getProperty("josm.test.data");
        if (testDataRoot == null || testDataRoot.isEmpty()) {
            testDataRoot = "test/data";
            System.out.println("System property josm.test.data is not set, using '" + testDataRoot + "'");
        }
        return testDataRoot.endsWith("/") ? testDataRoot : testDataRoot + "/";
    }

    public static OsmPrimitive createPrimitive(String assertion) {
        if (Main.pref == null) {
            Main.initApplicationPreferences();
        }
        final String[] x = assertion.split("\\s+", 2);
        final OsmPrimitive p = "n".equals(x[0]) || "node".equals(x[0])
                ? new Node()
                : "w".equals(x[0]) || "way".equals(x[0])
                ? new Way()
                : "r".equals(x[0]) || "relation".equals(x[0])
                ? new Relation()
                : null;
        if (p == null) {
            throw new IllegalArgumentException("Expecting n/node/w/way/r/relation, but got " + x[0]);
        }
        for (final Map.Entry<String, String> i : TextTagParser.readTagsFromText(x[1]).entrySet()) {
            p.put(i.getKey(), i.getValue());
        }
        return p;
    }

    @Test
    public void testCreatePrimitive() throws Exception {
        final OsmPrimitive p = createPrimitive("way name=Foo railway=rail");
        assertTrue(p instanceof Way);
        assertThat(p.keySet().size(), is(2));
        assertThat(p.get("name"), is("Foo"));
        assertThat(p.get("railway"), is("rail"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreatePrimitiveFail() throws Exception {
        TestUtils.createPrimitive("noway name=Foo");
    }

}
