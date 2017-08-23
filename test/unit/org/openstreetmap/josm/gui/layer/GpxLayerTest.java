// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.TimeZone;

import javax.swing.JScrollPane;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.ImmutableGpxTrack;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.io.GpxReaderTest;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.xml.sax.SAXException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link GpxLayer} class.
 */
public class GpxLayerTest {

    /**
     * Setup tests
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().platform().mainMenu().projection().i18n();

    private static String getHtml(GpxLayer layer) {
        return ((HtmlPanel) ((JScrollPane) layer.getInfoComponent()).getViewport().getView()).getEditorPane().getText();
    }

    /**
     * Returns minimal GPX data.
     * @return minimal GPX data, with a single waypoint, a single track composed of a single segment
     * @throws IOException if any I/O error occurs
     * @throws SAXException if any SAX error occurs
     */
    public static GpxData getMinimalGpxData() throws IOException, SAXException {
        return GpxReaderTest.parseGpxData(TestUtils.getTestDataRoot() + "minimal.gpx");
    }

    /**
     * Returns minimal GPX layer.
     * @return minimal GPX layer, with a single waypoint, a single track composed of a single segment
     * @throws IOException if any I/O error occurs
     * @throws SAXException if any SAX error occurs
     */
    public static GpxLayer getMinimalGpxLayer() throws IOException, SAXException {
        return new GpxLayer(getMinimalGpxData());
    }

    /**
     * Unit test of {@link GpxLayer#GpxLayer}.
     * @throws Exception if any error occurs
     */
    @Test
    public void testGpxLayer() throws Exception {
        GpxLayer layer = new GpxLayer(new GpxData(), "foo", false);
        assertEquals("foo", layer.getName());
        assertFalse(layer.isLocalFile());
        assertEquals(Color.MAGENTA, layer.getColorProperty().get());
        assertEquals("<html>0 tracks, 0 routes, 0 waypoints<br>Length: < 0.01 m<br></html>", layer.getToolTipText());

        GpxLayer layer2 = new GpxLayer(new GpxData(), "bar", true);
        assertEquals("bar", layer2.getName());
        assertTrue(layer2.isLocalFile());
        assertEquals(Color.MAGENTA, layer2.getColorProperty().get());
        assertEquals("<html>0 tracks, 0 routes, 0 waypoints<br>Length: < 0.01 m<br></html>", layer2.getToolTipText());

        assertTrue(layer.checkSaveConditions());
        assertTrue(layer.isInfoResizable());
        assertTrue(layer.isSavable());
        assertTrue(layer.isMergable(layer2));

        layer.projectionChanged(null, null);
        layer.projectionChanged(null, Projections.getProjectionByCode("EPSG:3857"));
    }

    /**
     * Unit test of {@link GpxLayer#getInfoComponent}.
     * @throws Exception if any error occurs
     */
    @Test
    public void testGetInfoComponent() throws Exception {
        assertEquals("<html>\n"+
                     "  <head>\n" +
                     "    \n" +
                     "  </head>\n" +
                     "  <body>\n" +
                     "    Length: 0.01 m<br>0 routes, 0 waypoints<br>\n" +
                     "  </body>\n" +
                     "</html>\n",
                     getHtml(new GpxLayer(new GpxData())));

        assertEquals("<html>\n"+
                     "  <head>\n" +
                     "    \n" +
                     "  </head>\n" +
                     "  <body>\n" +
                     "    <table>\n" +
                     "      <tr align=\"center\">\n" +
                     "        <td colspan=\"5\">\n" +
                     "          1 track\n" +
                     "        </td>\n" +
                     "      </tr>\n" +
                     "      <tr align=\"center\">\n" +
                     "        <td>\n" +
                     "          Name\n" +
                     "        </td>\n" +
                     "        <td>\n" +
                     "          Description\n" +
                     "        </td>\n" +
                     "        <td>\n" +
                     "          Timespan\n" +
                     "        </td>\n" +
                     "        <td>\n" +
                     "          Length\n" +
                     "        </td>\n" +
                     "        <td>\n" +
                     "          URL\n" +
                     "        </td>\n" +
                     "      </tr>\n" +
                     "      <tr>\n" +
                     "        <td>\n" +
                     "          2016-01-03 20:40:14\n" +
                     "        </td>\n" +
                     "        <td>\n" +
                     "          \n" +
                     "        </td>\n" +
                     "        <td>\n" +
                     "          1/3/16 11:59 AM - 12:00 PM (0:00)\n" +
                     "        </td>\n" +
                     "        <td>\n" +
                     "          12.0 m\n" +
                     "        </td>\n" +
                     "        <td>\n" +
                     "          \n" +
                     "        </td>\n" +
                     "      </tr>\n" +
                     "    </table>\n" +
                     "    <br>\n" +
                     "    <br>\n" +
                     "    Length: 12.0 m<br>0 routes, 1 waypoint<br>\n" +
                     "  </body>\n" +
                     "</html>\n",
                     getHtml(getMinimalGpxLayer()));
    }

    /**
     * Unit test of {@link GpxLayer#getTimespanForTrack}.
     * @throws Exception if any error occurs
     */
    @Test
    public void testGetTimespanForTrack() throws Exception {
        assertEquals("", GpxLayer.getTimespanForTrack(
                new ImmutableGpxTrack(new ArrayList<Collection<WayPoint>>(), new HashMap<String, Object>())));

        assertEquals("1/3/16 11:59 AM - 12:00 PM (0:00)", GpxLayer.getTimespanForTrack(getMinimalGpxData().tracks.iterator().next()));

        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"));
        assertEquals("1/3/16 12:59 PM - 1:00 PM (0:00)", GpxLayer.getTimespanForTrack(getMinimalGpxData().tracks.iterator().next()));
    }

    /**
     * Unit test of {@link GpxLayer#mergeFrom}.
     * @throws Exception if any error occurs
     */
    @Test
    public void testMergeFrom() throws Exception {
        GpxLayer layer = new GpxLayer(new GpxData());
        assertTrue(layer.data.isEmpty());
        layer.mergeFrom(getMinimalGpxLayer());
        assertFalse(layer.data.isEmpty());
        assertEquals(1, layer.data.tracks.size());
        assertEquals(1, layer.data.waypoints.size());
    }

    /**
     * Test that {@link GpxLayer#mergeFrom} throws IAE for invalid arguments
     */
    @Test(expected = IllegalArgumentException.class)
    public void testMergeFromIAE() {
        new GpxLayer(new GpxData()).mergeFrom(new OsmDataLayer(new DataSet(), "", null));
    }

    /**
     * Unit test of {@link GpxLayer#paint}.
     * @throws Exception if any error occurs
     */
    @Test
    public void testPaint() throws Exception {
        GpxLayer layer = getMinimalGpxLayer();
        try {
            Main.getLayerManager().addLayer(layer);
            assertTrue(layer.getMenuEntries().length > 0);
            layer.paint(TestUtils.newGraphics(), MainApplication.getMap().mapView, layer.data.getMetaBounds());
        } finally {
            Main.getLayerManager().removeLayer(layer);
        }
    }
}
