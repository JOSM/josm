// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.session;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.gui.preferences.projection.ProjectionPreference;
import org.openstreetmap.josm.tools.MultiMap;
import org.openstreetmap.josm.tools.Pair;

/**
 * Unit tests for Session writing.
 */
public class SessionWriterTest {

    private static class OsmHeadlessJosExporter extends OsmDataSessionExporter {
        public OsmHeadlessJosExporter(OsmDataLayer layer) {
            super(layer);
        }
        @Override
        public boolean requiresZip() {
            return false;
        }
    }

    private static class OsmHeadlessJozExporter extends OsmDataSessionExporter {
        public OsmHeadlessJozExporter(OsmDataLayer layer) {
            super(layer);
        }
        @Override
        public boolean requiresZip() {
            return true;
        }
    }

    private static class GpxHeadlessJosExporter extends GpxTracksSessionExporter {
        public GpxHeadlessJosExporter(GpxLayer layer) {
            super(layer);
        }

        @Override
        public boolean requiresZip() {
            return false;
        }
    }

    private static class GpxHeadlessJozExporter extends GpxTracksSessionExporter {
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
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init(true);
        ProjectionPreference.setProjection();
    }

    private void testWrite(List<Layer> layers, final boolean zip) throws IOException {
        Map<Layer, SessionLayerExporter> exporters = new HashMap<>();
        if (zip) {
            SessionWriter.registerSessionLayerExporter(OsmDataLayer.class, OsmHeadlessJozExporter.class);
            SessionWriter.registerSessionLayerExporter(GpxLayer.class, GpxHeadlessJozExporter.class);
        } else {
            SessionWriter.registerSessionLayerExporter(OsmDataLayer.class, OsmHeadlessJosExporter.class);
            SessionWriter.registerSessionLayerExporter(GpxLayer.class, GpxHeadlessJosExporter.class);
        }
        for (final Layer l : layers) {
            exporters.put(l, SessionWriter.getSessionLayerExporter(l));
        }
        SessionWriter sw = new SessionWriter(layers, -1, exporters, new MultiMap<Layer, Layer>(), zip);
        File file = new File(System.getProperty("java.io.tmpdir"), getClass().getName()+(zip?".joz":".jos"));
        try {
            sw.write(file);
        } finally {
            if (file.exists()) {
                file.delete();
            }
        }
    }

    private OsmDataLayer createOsmLayer() {
        OsmDataLayer layer = new OsmDataLayer(new DataSet(), "OSM layer name", null);
        layer.setAssociatedFile(new File("data.osm"));
        return layer;
    }

    private GpxLayer createGpxLayer() {
        GpxData data = new GpxData();
        data.waypoints.add(new WayPoint(new LatLon(42.72665, -0.00747)));
        data.waypoints.add(new WayPoint(new LatLon(42.72659, -0.00749)));
        GpxLayer layer = new GpxLayer(data, "GPX layer name");
        layer.setAssociatedFile(new File("data.gpx"));
        return layer;
    }

    private MarkerLayer createMarkerLayer(GpxLayer gpx) {
        return new MarkerLayer(gpx.data, "Marker layer name", gpx.getAssociatedFile(), gpx);
    }

    /**
     * Tests to write an empty .jos file.
     * @throws IOException if any I/O error occurs
     */
    @Test
    public void testWriteEmptyJos() throws IOException {
        testWrite(Collections.<Layer>emptyList(), false);
    }

    /**
     * Tests to write an empty .joz file.
     * @throws IOException if any I/O error occurs
     */
    @Test
    public void testWriteEmptyJoz() throws IOException {
        testWrite(Collections.<Layer>emptyList(), true);
    }

    /**
     * Tests to write a .jos file containing OSM data.
     * @throws IOException if any I/O error occurs
     */
    @Test
    public void testWriteOsmJos() throws IOException {
        testWrite(Collections.<Layer>singletonList(createOsmLayer()), false);
    }

    /**
     * Tests to write a .joz file containing OSM data.
     * @throws IOException if any I/O error occurs
     */
    @Test
    public void testWriteOsmJoz() throws IOException {
        testWrite(Collections.<Layer>singletonList(createOsmLayer()), true);
    }

    /**
     * Tests to write a .jos file containing GPX data.
     * @throws IOException if any I/O error occurs
     */
    @Test
    public void testWriteGpxJos() throws IOException {
        testWrite(Collections.<Layer>singletonList(createGpxLayer()), false);
    }

    /**
     * Tests to write a .joz file containing GPX data.
     * @throws IOException if any I/O error occurs
     */
    @Test
    public void testWriteGpxJoz() throws IOException {
        testWrite(Collections.<Layer>singletonList(createGpxLayer()), true);
    }

    /**
     * Tests to write a .joz file containing GPX and marker data.
     * @throws IOException if any I/O error occurs
     */
    @Test
    public void testWriteGpxAndMarkerJoz() throws IOException {
        GpxLayer gpx = createGpxLayer();
        testWrite(Pair.toList(new Pair<Layer, Layer>(gpx, createMarkerLayer(gpx))), true);
    }
}
