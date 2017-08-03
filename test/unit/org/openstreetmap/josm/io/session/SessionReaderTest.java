// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.TestUtils;
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
public class SessionReaderTest {

    /**
     * Setup tests.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().platform().projection();

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
    public void testReadEmpty() throws IOException, IllegalDataException {
        assertTrue(testRead("empty.jos").isEmpty());
        assertTrue(testRead("empty.joz").isEmpty());
    }

    /**
     * Tests to read a .jos or .joz file containing OSM data.
     * @throws IOException if any I/O error occurs
     * @throws IllegalDataException is the test file is considered as invalid
     */
    @Test
    public void testReadOsm() throws IOException, IllegalDataException {
        for (String file : new String[]{"osm.jos", "osm.joz"}) {
            List<Layer> layers = testRead(file);
            assertEquals(layers.size(), 1);
            assertTrue(layers.get(0) instanceof OsmDataLayer);
            OsmDataLayer osm = (OsmDataLayer) layers.get(0);
            assertEquals(osm.getName(), "OSM layer name");
        }
    }

    /**
     * Tests to read a .jos or .joz file containing GPX data.
     * @throws IOException if any I/O error occurs
     * @throws IllegalDataException is the test file is considered as invalid
     */
    @Test
    public void testReadGpx() throws IOException, IllegalDataException {
        for (String file : new String[]{"gpx.jos", "gpx.joz", "nmea.jos"}) {
            List<Layer> layers = testRead(file);
            assertEquals(layers.size(), 1);
            assertTrue(layers.get(0) instanceof GpxLayer);
            GpxLayer gpx = (GpxLayer) layers.get(0);
            assertEquals(gpx.getName(), "GPX layer name");
        }
    }

    /**
     * Tests to read a .joz file containing GPX and marker data.
     * @throws IOException if any I/O error occurs
     * @throws IllegalDataException is the test file is considered as invalid
     */
    @Test
    public void testReadGpxAndMarker() throws IOException, IllegalDataException {
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
    }

    /**
     * Tests to read a .jos file containing Bing imagery.
     * @throws IOException if any I/O error occurs
     * @throws IllegalDataException is the test file is considered as invalid
     */
    @Test
    public void testReadImage() throws IOException, IllegalDataException {
        final List<Layer> layers = testRead("bing.jos");
        assertEquals(layers.size(), 1);
        assertTrue(layers.get(0) instanceof ImageryLayer);
        final AbstractTileSourceLayer<?> image = (AbstractTileSourceLayer<?>) layers.get(0);
        assertEquals("Bing aerial imagery", image.getName());
        assertEquals(-2.671667778864503, image.getDisplaySettings().getDx(), 1e-9);
        assertEquals(13.89643478114158, image.getDisplaySettings().getDy(), 1e-9);
    }

    /**
     * Tests to read a .joz file containing notes.
     * @throws IOException if any I/O error occurs
     * @throws IllegalDataException is the test file is considered as invalid
     */
    @Test
    public void testReadNotes() throws IOException, IllegalDataException {
        if (Main.isDisplayingMapView()) {
            for (NoteLayer nl : Main.getLayerManager().getLayersOfType(NoteLayer.class)) {
                Main.getLayerManager().removeLayer(nl);
            }
        }
        final List<Layer> layers = testRead("notes.joz");
        assertEquals(layers.size(), 1);
        assertTrue(layers.get(0) instanceof NoteLayer);
        final NoteLayer layer = (NoteLayer) layers.get(0);
        assertEquals("Notes", layer.getName());
        assertEquals(174, layer.getNoteData().getNotes().size());
    }
}
