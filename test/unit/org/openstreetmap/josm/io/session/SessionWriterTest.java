// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.OffsetBookmark;
import org.openstreetmap.josm.data.notes.Note;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.ImageryLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.NoteLayer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.TMSLayer;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.Projection;
import org.openstreetmap.josm.tools.MultiMap;
import org.openstreetmap.josm.tools.Utils;

/**
 * Unit tests for Session writing.
 */
@BasicPreferences
@Main
@Projection
public class SessionWriterTest {

    protected static final class OsmHeadlessJosExporter extends OsmDataSessionExporter {
        public OsmHeadlessJosExporter(OsmDataLayer layer) {
            super(layer);
        }

        @Override
        public boolean requiresZip() {
            return false;
        }
    }

    protected static final class OsmHeadlessJozExporter extends OsmDataSessionExporter {
        public OsmHeadlessJozExporter(OsmDataLayer layer) {
            super(layer);
        }

        @Override
        public boolean requiresZip() {
            return true;
        }
    }

    protected static final class GpxHeadlessJosExporter extends GpxTracksSessionExporter {
        public GpxHeadlessJosExporter(GpxLayer layer) {
            super(layer);
        }

        @Override
        public boolean requiresZip() {
            return false;
        }
    }

    protected static final class GpxHeadlessJozExporter extends GpxTracksSessionExporter {
        public GpxHeadlessJozExporter(GpxLayer layer) {
            super(layer);
        }

        @Override
        public boolean requiresZip() {
            return true;
        }
    }

    /**
     * Setup tests.
     */
    @BeforeEach
    public void setUp() {
        MainApplication.getLayerManager().addLayer(createOsmLayer());
    }

    private Map<String, byte[]> testWrite(List<Layer> layers, final boolean zip) throws IOException {
        Map<Layer, SessionLayerExporter> exporters = new HashMap<>();
        if (zip) {
            SessionWriter.registerSessionLayerExporter(OsmDataLayer.class, OsmHeadlessJozExporter.class);
            SessionWriter.registerSessionLayerExporter(GpxLayer.class, GpxHeadlessJozExporter.class);
        } else {
            SessionWriter.registerSessionLayerExporter(OsmDataLayer.class, OsmHeadlessJosExporter.class);
            SessionWriter.registerSessionLayerExporter(GpxLayer.class, GpxHeadlessJosExporter.class);
        }
        for (final Layer l : layers) {
            SessionLayerExporter s = SessionWriter.getSessionLayerExporter(l);
            s.getExportPanel();
            exporters.put(l, s);
            if (s instanceof GpxTracksSessionExporter) {
                ((GpxTracksSessionExporter) s).setMetaTime(Instant.parse("2021-10-16T18:27:12.351Z"));
            } else if (s instanceof MarkerSessionExporter) {
                ((MarkerSessionExporter) s).setMetaTime(Instant.parse("2021-10-16T18:27:12.351Z"));
            }
        }
        SessionWriter sw = new SessionWriter(layers, -1, exporters, new MultiMap<Layer, Layer>(), zip);
        File file = new File(System.getProperty("java.io.tmpdir"), getClass().getName()+(zip ? ".joz" : ".jos"));
        try {
            sw.write(file);
            if (!zip) {
                return null;
            }
            try (ZipFile zipFile = new ZipFile(file)) {
                return Collections.list(zipFile.entries()).stream().collect(Collectors.toMap(ZipEntry::getName, e -> {
                    try {
                        return Utils.readBytesFromStream(zipFile.getInputStream(e));
                    } catch (IOException ex) {
                        fail(ex);
                    }
                    return null;
                }));
            }
        } finally {
            if (file.exists()) {
                Utils.deleteFile(file);
            }
        }
    }

    /**
     * Creates an OSM layer
     * @return OSM layer
     * @since 18466
     */
    public static OsmDataLayer createOsmLayer() {
        OsmDataLayer layer = new OsmDataLayer(new DataSet(), "OSM layer name", null);
        layer.setAssociatedFile(new File("data.osm"));
        return layer;
    }

    /**
     * Creates a GPX layer
     * @return GPX layer
     * @since 18466
     */
    public static GpxLayer createGpxLayer() {
        GpxData data = new GpxData();
        WayPoint wp = new WayPoint(new LatLon(42.72665, -0.00747));
        wp.setInstant(Instant.parse("2021-01-01T10:15:30.00Z"));
        data.waypoints.add(wp);
        data.waypoints.add(new WayPoint(new LatLon(42.72659, -0.00749)));
        GpxLayer layer = new GpxLayer(data, "GPX layer name");
        layer.setAssociatedFile(new File("data.gpx"));
        return layer;
    }

    /**
     * Creates a MarkerLayer
     * @param gpx linked GPX layer
     * @return MarkerLayer
     * @since 18466
     */
    public static MarkerLayer createMarkerLayer(GpxLayer gpx) {
        MarkerLayer layer = new MarkerLayer(gpx.data, "Marker layer name", gpx.getAssociatedFile(), gpx);
        layer.setOpacity(0.5);
        layer.setColor(new Color(0x12345678, true));
        gpx.setLinkedMarkerLayer(layer);
        return layer;
    }

    /**
     * Creates an ImageryLayer
     * @return ImageryLayer
     * @since 18466
     */
    public static ImageryLayer createImageryLayer() {
        TMSLayer layer = new TMSLayer(new ImageryInfo("the name", "http://www.url.com/"));
        layer.getDisplaySettings().setOffsetBookmark(
                new OffsetBookmark(ProjectionRegistry.getProjection().toCode(), layer.getInfo().getId(), layer.getInfo().getName(), "", 12, 34));
        return layer;
    }

    /**
     * Creates a NoteLayer
     * @return NoteLayer
     * @since 18466
     */
    public static NoteLayer createNoteLayer() {
        return new NoteLayer(Arrays.asList(new Note(LatLon.ZERO)), "layer name");
    }

    /**
     * Tests to write an empty .jos file.
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testWriteEmptyJos() throws IOException {
        testWrite(Collections.<Layer>emptyList(), false);
    }

    /**
     * Tests to write an empty .joz file.
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testWriteEmptyJoz() throws IOException {
        testWrite(Collections.<Layer>emptyList(), true);
    }

    /**
     * Tests to write a .jos file containing OSM data.
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testWriteOsmJos() throws IOException {
        testWrite(Collections.<Layer>singletonList(createOsmLayer()), false);
    }

    /**
     * Tests to write a .joz file containing OSM data.
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testWriteOsmJoz() throws IOException {
        testWrite(Collections.<Layer>singletonList(createOsmLayer()), true);
    }

    /**
     * Tests to write a .jos file containing GPX data.
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testWriteGpxJos() throws IOException {
        testWrite(Collections.<Layer>singletonList(createGpxLayer()), false);
    }

    /**
     * Tests to write a .joz file containing GPX data.
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testWriteGpxJoz() throws IOException {
        testWrite(Collections.<Layer>singletonList(createGpxLayer()), true);
    }

    /**
     * Tests to write a .joz file containing GPX and marker data.
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testWriteGpxAndMarkerJoz() throws IOException {
        GpxLayer gpx = createGpxLayer();
        MarkerLayer markers = createMarkerLayer(gpx);
        Map<String, byte[]> bytes = testWrite(Arrays.asList(gpx, markers), true);

        Path path = Paths.get(TestUtils.getTestDataRoot() + "/sessions/gpx_markers_combined.jos");
        String expected = new String(Files.readAllBytes(path), StandardCharsets.UTF_8).replace("\r", "");
        String actual = new String(bytes.get("session.jos"), StandardCharsets.UTF_8).replace("\r", "");
        assertEquals(expected, actual);

        path = Paths.get(TestUtils.getTestDataRoot() + "/sessions/data_export.gpx");
        expected = new String(Files.readAllBytes(path), StandardCharsets.UTF_8).replace("\r", "");
        actual = new String(bytes.get("layers/01/data.gpx"), StandardCharsets.UTF_8).replace("\r", "");
        assertEquals(expected, actual);

        //Test writing when the marker layer has no corresponding GPX layer:
        gpx.setLinkedMarkerLayer(null);
        markers.fromLayer = null;
        markers.data.transferLayerPrefs(gpx.data.getLayerPrefs());
        bytes = testWrite(Arrays.asList(gpx, markers), true);

        path = Paths.get(TestUtils.getTestDataRoot() + "/sessions/gpx_markers.jos");
        expected = new String(Files.readAllBytes(path), StandardCharsets.UTF_8).replace("\r", "");
        actual = new String(bytes.get("session.jos"), StandardCharsets.UTF_8).replace("\r", "");
        assertEquals(expected, actual);

        path = Paths.get(TestUtils.getTestDataRoot() + "/sessions/data_export.gpx");
        expected = new String(Files.readAllBytes(path), StandardCharsets.UTF_8).replace("\r", "");
        actual = new String(bytes.get("layers/01/data.gpx"), StandardCharsets.UTF_8).replace("\r", "");
        assertEquals(expected, actual);

        path = Paths.get(TestUtils.getTestDataRoot() + "/sessions/markers.gpx");
        expected = new String(Files.readAllBytes(path), StandardCharsets.UTF_8).replace("\r", "");
        actual = new String(bytes.get("layers/02/data.gpx"), StandardCharsets.UTF_8).replace("\r", "");
        assertEquals(expected, actual);

    }

    /**
     * Tests to write a .joz file containing an imagery layer.
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testWriteImageryLayer() throws IOException {
        final Layer layer = createImageryLayer();
        testWrite(Collections.singletonList(layer), true);
    }

    /**
     * Tests to write a .joz file containing a note layer.
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testWriteNoteLayer() throws IOException {
        final Layer layer = createNoteLayer();
        testWrite(Collections.singletonList(layer), true);
    }
}
