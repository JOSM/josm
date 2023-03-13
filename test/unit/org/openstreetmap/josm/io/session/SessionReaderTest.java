// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.AbstractTileSourceLayer;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.ImageryLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.NoteLayer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for Session reading.
 */
class SessionReaderTest {

    /**
     * Setup tests.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().projection();

    private static String getSessionDataDir() {
        return TestUtils.getTestDataRoot() + "/sessions";
    }

    private List<Layer> testRead(String sessionFileName) throws IOException, IllegalDataException {
        boolean zip = sessionFileName.endsWith(".joz");
        File file = new File(getSessionDataDir(), sessionFileName);
        SessionReader reader = new SessionReader();
        reader.loadSession(file, zip, null);
        return reader.getLayers();
    }

    /**
     * Tests to read an empty .jos or .joz file.
     * @throws IOException if any I/O error occurs
     * @throws IllegalDataException is the test file is considered as invalid
     */
    @Test
    void testReadEmpty() throws IOException, IllegalDataException {
        assertTrue(testRead("empty.jos").isEmpty());
        assertTrue(testRead("empty.joz").isEmpty());
    }

    /**
     * Tests to read a .jos or .joz file containing OSM data.
     * @throws IOException if any I/O error occurs
     * @throws IllegalDataException is the test file is considered as invalid
     */
    @Test
    void testReadOsm() throws IOException, IllegalDataException {
        for (String file : new String[]{"osm.jos", "osm.joz"}) {
            List<Layer> layers = testRead(file);
            assertEquals(layers.size(), 1);
            OsmDataLayer osm = assertInstanceOf(OsmDataLayer.class, layers.get(0));
            assertEquals(osm.getName(), "OSM layer name");
        }
    }

    /**
     * Tests to read a .jos or .joz file containing GPX data.
     * @throws IOException if any I/O error occurs
     * @throws IllegalDataException is the test file is considered as invalid
     */
    @Test
    void testReadGpx() throws IOException, IllegalDataException {
        for (String file : new String[]{"gpx.jos", "gpx.joz", "nmea.jos"}) {
            List<Layer> layers = testRead(file);
            assertEquals(layers.size(), 1);
            GpxLayer gpx = assertInstanceOf(GpxLayer.class, layers.get(0));
            assertEquals(gpx.getName(), "GPX layer name");
        }
    }

    /**
     * Tests to read a .joz file containing GPX and marker data.
     * @throws IOException if any I/O error occurs
     * @throws IllegalDataException is the test file is considered as invalid
     */
    @Test
    void testReadGpxAndMarker() throws IOException, IllegalDataException {
        List<Layer> layers = testRead("gpx_markers.joz");
        assertEquals(layers.size(), 2);
        GpxLayer gpx = null;
        MarkerLayer marker = null;
        for (Layer layer : layers) {
            if (layer instanceof GpxLayer) {
                gpx = (GpxLayer) layer;
            } else if (layer instanceof MarkerLayer) {
                marker = (MarkerLayer) layer;
            }
        }
        assertNotNull(gpx);
        assertNotNull(marker);
        assertEquals(gpx.getName(), "GPX layer name");
        assertEquals(marker.getName(), "Marker layer name");
        assertEquals(1.0, gpx.getOpacity());
        assertEquals(0.5, marker.getOpacity());
        assertEquals(new Color(0x204060), gpx.getColor());
        assertEquals(new Color(0x12345678, true), marker.getColor());
    }

    /**
     * Tests to read a .jos file containing Bing imagery.
     * @throws IOException if any I/O error occurs
     * @throws IllegalDataException is the test file is considered as invalid
     */
    @Test
    void testReadImage() throws IOException, IllegalDataException {
        final List<Layer> layers = testRead("bing.jos");
        assertEquals(layers.size(), 1);
        assertInstanceOf(ImageryLayer.class, layers.get(0));
        final AbstractTileSourceLayer<?> image = (AbstractTileSourceLayer<?>) layers.get(0);
        assertEquals("Bing aerial imagery", image.getName());
        EastNorth displacement = image.getDisplaySettings().getDisplacement();
        assertEquals(-2.671667778864503, displacement.east(), 1e-9);
        assertEquals(13.89643478114158, displacement.north(), 1e-9);
    }

    /**
     * Tests to read a .joz file containing notes.
     * @throws IOException if any I/O error occurs
     * @throws IllegalDataException is the test file is considered as invalid
     */
    @Test
    void testReadNotes() throws IOException, IllegalDataException {
        if (MainApplication.isDisplayingMapView()) {
            for (NoteLayer nl : MainApplication.getLayerManager().getLayersOfType(NoteLayer.class)) {
                MainApplication.getLayerManager().removeLayer(nl);
            }
        }
        final List<Layer> layers = testRead("notes.joz");
        assertEquals(layers.size(), 1);
        final NoteLayer layer = assertInstanceOf(NoteLayer.class, layers.get(0));
        assertEquals("Notes", layer.getName());
        assertEquals(174, layer.getNoteData().getNotes().size());
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/17701">Bug #17701</a>.
     * @throws Exception if an error occurs
     */
    @Test
    void testTicket17701() throws Exception {
        try (InputStream in = new ByteArrayInputStream(("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<josm-session version=\"0.1\">\n" +
                "    <layers active=\"1\">\n" +
                "        <layer index=\"1\" name=\"GPS-треки OpenStreetMap\" type=\"imagery\" version=\"0.1\" visible=\"true\">\n" +
                "            <id>osm-gps</id>\n" +
                "            <type>tms</type>\n" +
                "            <url>https://{switch:a,b,c}.gps-tile.openstreetmap.org/lines/{zoom}/{x}/{y}.png</url>\n" +
                "            <attribution-text>© OpenStreetMap contributors</attribution-text>\n" +
                "            <attribution-url>https://www.openstreetmap.org/copyright</attribution-url>\n" +
                "            <max-zoom>20</max-zoom>\n" +
                "            <cookies/>\n" +
                "            <description>Общедоступные GPS-треки, загруженные на OpenStreetMap.</description>\n" +
                "            <valid-georeference>true</valid-georeference>\n" +
                "            <overlay>true</overlay>\n" +
                "            <show-errors>true</show-errors>\n" +
                "            <automatic-downloading>true</automatic-downloading>\n" +
                "            <automatically-change-resolution>true</automatically-change-resolution>\n" +
                "        </layer>\r\n" +
                "    </layers>\n" +
                "</josm-session>").getBytes(StandardCharsets.UTF_8))) {
            SessionReader reader = new SessionReader();
            reader.loadSession(in, null, false, null);
            assertTrue(reader.getLayers().isEmpty());
        }
    }
}
