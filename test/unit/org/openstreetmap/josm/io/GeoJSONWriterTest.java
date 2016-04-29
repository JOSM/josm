// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.preferences.projection.ProjectionPreference;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class GeoJSONWriterTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    @Test
    public void testPoint() throws Exception {
        final Node node = new Node(new LatLon(12.3, 4.56));
        node.put("name", "foo");
        node.put("source", "code");
        final DataSet ds = new DataSet();
        ds.addPrimitive(node);
        final OsmDataLayer layer = new OsmDataLayer(ds, "foo", null);
        final GeoJSONWriter writer = new GeoJSONWriter(layer, ProjectionPreference.wgs84.getProjection());
        assertThat(writer.write().trim(), is(("" +
                "{\n" +
                "    'type':'FeatureCollection',\n" +
                "    'crs':{\n" +
                "        'type':'name',\n" +
                "        'properties':{\n" +
                "            'name':'EPSG:4326'\n" +
                "        }\n" +
                "    },\n" +
                "    'generator':'JOSM',\n" +
                "    'features':[\n" +
                "        {\n" +
                "            'type':'Feature',\n" +
                "            'properties':{\n" +
                "                'name':'foo',\n" +
                "                'source':'code'\n" +
                "            },\n" +
                "            'geometry':{\n" +
                "                'type':'Point',\n" +
                "                'coordinates':[\n" +
                "                    4.56000000000,\n" +
                "                    12.30000000000\n" +
                "                ]\n" +
                "            }\n" +
                "        }\n" +
                "    ]\n" +
                "}").replace("'", "\"")));
    }
}
