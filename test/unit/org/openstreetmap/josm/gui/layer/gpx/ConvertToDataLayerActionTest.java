// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.gpx;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.TagMap;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.io.GpxReaderTest;

/**
 * Unit tests of {@link ConvertToDataLayerAction} class.
 */
public class ConvertToDataLayerActionTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init(false);
    }

    /**
     * Tests a conversion from a GPX marker layer to a OSM dataset
     * @throws Exception if the parsing fails
     */
    @Test
    public void testFromMarkerLayer() throws Exception {
        final GpxData data = GpxReaderTest.parseGpxData(TestUtils.getTestDataRoot() + "minimal.gpx");
        final MarkerLayer markers = new MarkerLayer(data, "Markers", data.storageFile, null);
        final DataSet osm = new ConvertToDataLayerAction.FromMarkerLayer(markers).convert();
        assertEquals(1, osm.getNodes().size());
        assertEquals(new TagMap("name", "Schranke", "description", "Pfad", "note", "Pfad", "gpxicon", "Toll Booth"),
                osm.getNodes().iterator().next().getKeys());
    }
}
