// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;

/**
 * Unit tests of {@link GeoJSONWriter} class.
 */
public class GeoJSONWriterTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test for Point
     */
    @Test
    public void testPoint() {
        final Node node = new Node(new LatLon(12.3, 4.56));
        node.put("name", "foo");
        node.put("source", "code");
        node.put("data", "{\"foo\": 1, \"bar\": \"baz\"}");
        final DataSet ds = new DataSet();
        ds.addPrimitive(node);
        final GeoJSONWriter writer = new GeoJSONWriter(ds);
        assertEquals(("" +
                "{\n" +
                "    'type': 'FeatureCollection',\n" +
                "    'generator': 'JOSM',\n" +
                "    'features': [\n" +
                "        {\n" +
                "            'type': 'Feature',\n" +
                "            'properties': {\n" +
                "                'name': 'foo',\n" +
                "                'source': 'code',\n" +
                "                'data': {\n" +
                "                    'foo': 1,\n" +
                "                    'bar': 'baz'\n" +
                "                }\n" +
                "            },\n" +
                "            'geometry': {\n" +
                "                'type': 'Point',\n" +
                "                'coordinates': [\n" +
                "                    4.56000000000,\n" +
                "                    12.30000000000\n" +
                "                ]\n" +
                "            }\n" +
                "        }\n" +
                "    ]\n" +
                "}").replace("'", "\""), writer.write().trim());
    }

    /**
     * Unit test for LineString
     */
    @Test
    public void testLineString() {
        final DataSet ds = new DataSet();
        final Node n1 = new Node(new LatLon(12.3, 4.56));
        final Node n2 = new Node(new LatLon(12.4, 4.57));
        ds.addPrimitive(n1);
        ds.addPrimitive(n2);
        final Way way = new Way();
        way.put("highway", "footway");
        way.setNodes(Arrays.asList(n1, n2));
        ds.addPrimitive(way);
        final GeoJSONWriter writer = new GeoJSONWriter(ds);
        assertEquals(("" +
                "{\n" +
                "    'type': 'FeatureCollection',\n" +
                "    'generator': 'JOSM',\n" +
                "    'features': [\n" +
                "        {\n" +
                "            'type': 'Feature',\n" +
                "            'properties': {\n" +
                "                'highway': 'footway'\n" +
                "            },\n" +
                "            'geometry': {\n" +
                "                'type': 'LineString',\n" +
                "                'coordinates': [\n" +
                "                    [\n" +
                "                        4.56000000000,\n" +
                "                        12.30000000000\n" +
                "                    ],\n" +
                "                    [\n" +
                "                        4.57000000000,\n" +
                "                        12.40000000000\n" +
                "                    ]\n" +
                "                ]\n" +
                "            }\n" +
                "        }\n" +
                "    ]\n" +
                "}").replace("'", "\""), writer.write().trim());
    }

    /**
     * Unit test for multipolygon
     * @throws Exception if an error occurs
     */
    @Test
    public void testMultipolygon() throws Exception {
        try (InputStream in = Files.newInputStream(Paths.get(TestUtils.getTestDataRoot(), "multipolygon.osm"))) {
            DataSet ds = OsmReader.parseDataSet(in, null);
            final GeoJSONWriter writer = new GeoJSONWriter(ds);
            assertTrue(writer.write().contains("MultiPolygon"));
        }
    }

    /**
     * Unit test for exporting invalid multipolygons, see #13827
     * @throws Exception if an error occurs
     */
    @Test
    public void testMultipolygonRobustness() throws Exception {
        try (InputStream in = Files.newInputStream(Paths.get("nodist/data/multipolygon.osm"))) {
            DataSet ds = OsmReader.parseDataSet(in, null);
            final GeoJSONWriter writer = new GeoJSONWriter(ds);
            assertTrue(writer.write().contains("MultiPolygon"));
        }
    }
}
