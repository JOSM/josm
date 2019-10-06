// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.gpx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.TagMap;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.io.GpxReaderTest;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.spi.preferences.Config;
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
        final DataSet osm = new ConvertFromMarkerLayerAction(markers).convert();
        assertEquals(1, osm.getNodes().size());
        assertEquals(new TagMap("name", "Schranke", "description", "Pfad", "note", "Pfad", "gpxicon", "Toll Booth"),
                osm.getNodes().iterator().next().getKeys());
    }

    /**
     * Tests conversions from GPX tracks to OSM datasets
     * @throws Exception if the parsing fails
     */
    @Test
    public void testFromTrack() throws Exception {
        Config.getPref().put("gpx.convert-tags", "no");
        testFromTrack("tracks");

        Config.getPref().put("gpx.convert-tags", "yes");
        testFromTrack("tracks-ele-time");

        Config.getPref().put("gpx.convert-tags", "list");
        Config.getPref().putList("gpx.convert-tags.list.yes", Arrays.asList("ele"));
        Config.getPref().putList("gpx.convert-tags.list.no", Arrays.asList("time"));
        testFromTrack("tracks-ele");


        Config.getPref().putList("gpx.convert-tags.list.yes", Arrays.asList("time"));
        Config.getPref().putList("gpx.convert-tags.list.no", Arrays.asList("ele"));
        testFromTrack("tracks-time");
    }

    private static class GenericNode {
        final LatLon coor;
        final Map<String, String> tags;

        GenericNode(Node n) {
            coor = n.getCoor().getRoundedToOsmPrecision();
            tags = n.getKeys();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof GenericNode)) {
                return false;
            }
            GenericNode other = (GenericNode) obj;
            return coor.equals(other.coor) && tags.equals(other.tags);
        }

        @Override
        public int hashCode() {
            return Objects.hash(coor, tags);
        }
    }

    private void testFromTrack(String expected) throws IOException, SAXException, IllegalDataException {
        final GpxData data = GpxReaderTest.parseGpxData(TestUtils.getTestDataRoot() + "tracks/tracks.gpx");
        final DataSet osmExpected = OsmReader.parseDataSet(Files.newInputStream(
                Paths.get(TestUtils.getTestDataRoot(), "tracks/" + expected + ".osm")), null);
        final GpxLayer layer = new GpxLayer(data);
        final DataSet osm = new ConvertFromGpxLayerAction(layer).convert();
        //compare sorted coordinates/tags and total amount of primitives, because IDs and order will vary after reload

        List<GenericNode> nodes = osm.getNodes().stream()
                .map(GenericNode::new)
                .sorted(Comparator.comparing(g -> g.coor.hashCode()))
                .collect(Collectors.toList());

        List<GenericNode> nodesExpected = osmExpected.getNodes().stream()
                .map(GenericNode::new)
                .sorted(Comparator.comparing(g -> g.coor.hashCode()))
                .collect(Collectors.toList());

        assertEquals(nodesExpected, nodes);
        assertEquals(osmExpected.allPrimitives().size(), osm.allPrimitives().size());
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
