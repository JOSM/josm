// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.gpx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.TagMap;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.io.GpxReaderTest;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.xml.sax.SAXException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link ConvertToDataLayerAction} class.
 */
public class ConvertToDataLayerActionTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

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

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/14275">#14275</a>
     * @throws IOException if an error occurs during reading
     * @throws SAXException if any XML error occurs
     */
    @Test
    public void testTicket14275() throws IOException, SAXException {
        assertNotNull(GpxReaderTest.parseGpxData(TestUtils.getRegressionDataFile(14275, "1485101437.8189685.gpx")));
    }
}
