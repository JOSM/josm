// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Component;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

import javax.swing.JScrollPane;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.IGpxTrackSegment;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.data.projection.CustomProjection;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.io.GpxReaderTest;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.date.DateUtils;
import org.xml.sax.SAXException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link GpxLayer} class.
 */
public class GpxLayerTest {

    /**
     * Setup tests
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().main().projection().i18n().metricSystem();

    /**
     * Setup test.
     */
    @BeforeEach
    void setUp() {
        Locale.setDefault(Locale.ROOT);
        DateUtils.PROP_ISO_DATES.put(true);
    }

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
        return new GpxLayer(getMinimalGpxData(), "Bananas");
    }

    /**
     * Unit test of {@link GpxLayer#GpxLayer}.
     */
    @Test
    void testGpxLayer() {
        GpxLayer layer = new GpxLayer(new GpxData(), "foo", false);
        GpxTrack trk = new GpxTrack(new ArrayList<IGpxTrackSegment>(), new HashMap<>());
        trk.getExtensions().add("gpxd", "color", "#FF0000");
        layer.data.addTrack(trk);

        assertEquals("foo", layer.getName());
        assertFalse(layer.isLocalFile());
        assertEquals(layer.getColor(), Color.RED);
        assertEquals("<html>1 track (0 segments), 0 routes, 0 waypoints<br>Length: < 0.01 m<br></html>", layer.getToolTipText());

        GpxLayer layer2 = new GpxLayer(new GpxData(), "bar", true);
        assertEquals("bar", layer2.getName());
        assertTrue(layer2.isLocalFile());
        assertNull(layer2.getColor());
        assertEquals("<html>0 tracks (0 segments), 0 routes, 0 waypoints<br>Length: < 0.01 m<br></html>", layer2.getToolTipText());

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
    void testGetInfoComponent() throws Exception {
        assertEquals("<html>\n"+
                     "  <head>\n" +
                     "    <style type=\"text/css\">\n" +
                     "      <!--\n" +
                     "        td { padding-top: 4px; padding-bottom: 4px; padding-right: 16px; padding-left: 16px }\n" +
                     "      -->\n" +
                     "    </style>\n" +
                     "    \n" +
                     "  </head>\n" +
                     "  <body>\n" +
                     "    Length: 0.01 m<br>0 routes, 0 waypoints<br>\n" +
                     "  </body>\n" +
                     "</html>\n",
                     getHtml(new GpxLayer(new GpxData())));

        assertEquals("<html>\n"+
                     "  <head>\n" +
                     "    <style type=\"text/css\">\n" +
                     "      <!--\n" +
                     "        td { padding-top: 4px; padding-bottom: 4px; padding-right: 16px; padding-left: 16px }\n" +
                     "      -->\n" +
                     "    </style>\n" +
                     "    \n" +
                     "  </head>\n" +
                     "  <body>\n" +
                     "    Creator: MapSource 6.16.3<br>\n\n" +
                     "    <table>\n" +
                     "      <tr align=\"center\">\n" +
                     "        <td colspan=\"5\">\n" +
                     "          1 track, 1 track segments\n" +
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
                     "          Number of<br>Segments\n" +
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
                     "          2016-01-03 11:59:58 &#8211; 12:00:00 (2.0 s)\n" +
                     "        </td>\n" +
                     "        <td>\n" +
                     "          12.0 m\n" +
                     "        </td>\n" +
                     "        <td>\n" +
                     "          1\n" +
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
    void testGetTimespanForTrack() throws Exception {
        assertEquals("", GpxLayer.getTimespanForTrack(
                new GpxTrack(new ArrayList<Collection<WayPoint>>(), new HashMap<>())));

        assertEquals("2016-01-03 11:59:58 \u2013 12:00:00 (2.0 s)", GpxLayer.getTimespanForTrack(getMinimalGpxData().tracks.iterator().next()));

        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"));
        assertEquals("2016-01-03 12:59:58 \u2013 13:00:00 (2.0 s)", GpxLayer.getTimespanForTrack(getMinimalGpxData().tracks.iterator().next()));
    }

    /**
     * Unit test of {@link GpxLayer#mergeFrom}.
     * @throws Exception if any error occurs
     */
    @Test
    void testMergeFrom() throws Exception {
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
    @Test
    void testMergeFromIAE() {
        final GpxLayer gpxLayer = new GpxLayer(new GpxData());
        final OsmDataLayer osmDataLayer = new OsmDataLayer(new DataSet(), "testMergeFromIAE", null);
        assertThrows(IllegalArgumentException.class, () -> gpxLayer.mergeFrom(osmDataLayer));
    }

    /**
     * Unit test of {@link GpxLayer#paint}.
     * @throws Exception if any error occurs
     */
    @Test
    void testPaint() throws Exception {
        GpxLayer layer = getMinimalGpxLayer();
        try {
            MainApplication.getLayerManager().addLayer(layer);
            assertTrue(layer.getMenuEntries().length > 0);
            layer.paint(TestUtils.newGraphics(), MainApplication.getMap().mapView, layer.data.getMetaBounds());
        } finally {
            MainApplication.getLayerManager().removeLayer(layer);
        }
    }

    /**
     * Unit test of {@link GpxLayer#getChangesetSourceTag}.
     */
    @Test
    void testGetChangesetSourceTag() {
        assertEquals("survey", new GpxLayer(new GpxData(), "", true).getChangesetSourceTag());
        assertNull(new GpxLayer(new GpxData(), "", false).getChangesetSourceTag());
    }

    /**
     * Checks that potential operations that could be called after destroy() are harmless
     */
    @Test
    void testRobustnessAfterDestroy() {
        GpxData data = new GpxData();
        GpxLayer layer = new GpxLayer(data, "1", false);
        GpxLayer otherLayer = new GpxLayer(new GpxData(), "2", false);
        assertEquals(data, layer.getData());
        assertTrue(layer.isMergable(otherLayer));
        assertTrue(layer.hasColor());
        assertTrue(layer.isSavable());
        assertTrue(layer.checkSaveConditions());
        assertFalse(layer.isModified());
        assertFalse(layer.requiresSaveToFile());
        assertNull(layer.getChangesetSourceTag());
        assertNull(layer.getAssociatedFile());

        layer.destroy();

        assertNull(layer.getData());
        assertNull(layer.getColor());
        assertFalse(layer.hasColor());
        assertFalse(layer.isMergable(otherLayer));
        assertFalse(layer.isSavable());
        assertFalse(layer.checkSaveConditions());
        assertFalse(layer.isModified());
        assertFalse(layer.requiresSaveToFile());
        assertNull(layer.getChangesetSourceTag());
        assertNull(layer.getAssociatedFile());
        Object infoComponent = layer.getInfoComponent();
        Component view = assertInstanceOf(JScrollPane.class, infoComponent).getViewport().getView();
        String text = assertInstanceOf(HtmlPanel.class, view).getEditorPane().getText().trim();
        assertTrue(text.startsWith("<html>"), text);
        assertTrue(text.endsWith("</html>"), text);
        assertEquals("<html><br></html>", layer.getToolTipText());
        assertDoesNotThrow(layer::jumpToNextMarker);
        assertDoesNotThrow(layer::jumpToPreviousMarker);
        assertDoesNotThrow(() -> layer.visitBoundingBox(new BoundingXYVisitor()));
        assertDoesNotThrow(() -> layer.filterTracksByDate(null, null, false));
        assertDoesNotThrow(() -> layer.projectionChanged(new CustomProjection(), new CustomProjection()));
    }
}
