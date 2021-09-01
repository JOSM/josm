// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.imagery;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.apache.commons.jcs3.access.behavior.ICacheAccess;
import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoaderListener;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.actions.ExpertToggleAction;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.TileJobOptions;
import org.openstreetmap.josm.data.imagery.vectortile.mapbox.MVTFile;
import org.openstreetmap.josm.data.imagery.vectortile.mapbox.MVTTile;
import org.openstreetmap.josm.data.imagery.vectortile.mapbox.MapboxVectorCachedTileLoader;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.testutils.FakeGraphics;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Test class for {@link MVTLayer}
 */
@BasicPreferences
class MVTLayerTest {
    // Needed for setting HTTP factory and the main window/mapview
    @RegisterExtension
    JOSMTestRules josmTestRules = new JOSMTestRules().main().projection();

    MVTLayer testLayer;

    @BeforeEach
    void setUp() {
        final ImageryInfo imageryInfo = new ImageryInfo("MvtLayerTest", "file:" + TestUtils.getTestDataRoot() + "pbf/mapillary/{z}/{x}/{y}.mvt");
        imageryInfo.setImageryType(ImageryInfo.ImageryType.MVT);
        this.testLayer = new MVTLayer(imageryInfo);
    }

    @Test
    void getTileLoaderClass() {
        assertEquals(MapboxVectorCachedTileLoader.class, this.testLayer.getTileLoaderClass());
    }

    @Test
    void getCacheName() {
        assertEquals("MVT", this.testLayer.getCacheName());
    }

    @Test
    void getCache() {
        assertNotNull(MVTLayer.getCache());
    }

    @Test
    void getNativeProjections() {
        assertArrayEquals(Collections.singleton(MVTFile.DEFAULT_PROJECTION).toArray(), this.testLayer.getNativeProjections().toArray());
    }

    /**
     * This is a non-regression test for JOSM #21260
     * @param projectionCode The projection code to use
     * @throws ReflectiveOperationException If the required method was unable to be called
     */
    @ParameterizedTest
    @ValueSource(strings = {"EPSG:3857" /* WGS 84 */, "EPSG:4326" /* Mercator (default) */, "EPSG:32612" /* UTM 12 N */})
    void ensureDifferentProjectionsAreFetched(final String projectionCode) throws ReflectiveOperationException {
        final Projection originalProjection = ProjectionRegistry.getProjection();
        try {
            ProjectionRegistry.setProjection(Projections.getProjectionByCode(projectionCode));
            // Needed to initialize mapView
            MainApplication.getLayerManager().addLayer(this.testLayer);
            final BBox tileBBox = new MVTTile(this.testLayer.getTileSource(), 3248, 6258, 14).getBBox();
            MainApplication.getMap().mapView.zoomTo(new Bounds(tileBBox.getMinLat(), tileBBox.getMinLon(),
                    tileBBox.getMaxLat(), tileBBox.getMaxLon()));
            final FakeGraphics graphics2D = new FakeGraphics();
            graphics2D.setClip(0, 0, 100, 100);
            this.testLayer.setZoomLevel(14);
            this.testLayer.getDisplaySettings().setAutoZoom(false);
            MainApplication.getMap().mapView.paintLayer(this.testLayer, graphics2D);
            Awaitility.await().atMost(Durations.FIVE_SECONDS).until(() -> !this.testLayer.getData().allPrimitives().isEmpty());
            assertFalse(this.testLayer.getData().allPrimitives().isEmpty());
        } finally {
            ProjectionRegistry.setProjection(originalProjection);
        }
    }

    @Test
    void getTileSource() {
        assertEquals(this.testLayer.getInfo().getUrl(), this.testLayer.getTileSource().getBaseUrl());
    }

    @Test
    void createTile() {
        assertNotNull(this.testLayer.createTile(this.testLayer.getTileSource(), 3251, 6258, 14));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void getMenuEntries(final boolean isExpert) {
        ExpertToggleAction.getInstance().setExpert(isExpert);
        // For now, just ensure that nothing throws on implementation
        MainApplication.getLayerManager().addLayer(this.testLayer);
        assertNotNull(assertDoesNotThrow(() -> this.testLayer.getMenuEntries()));
    }

    @Test
    void getData() {
        assertNotNull(this.testLayer.getData());
    }

    @Test
    void finishedLoading() throws ReflectiveOperationException {
        final MVTTile mvtTile = (MVTTile) this.testLayer.createTile(this.testLayer.getTileSource(), 3248, 6258, 14);
        final FinishedLoading finishedLoading = new FinishedLoading();
        mvtTile.addTileLoaderFinisher(finishedLoading);
        assertTrue(this.testLayer.getData().allPrimitives().isEmpty());
        this.testLayer.getTileLoaderClass().getConstructor(TileLoaderListener.class, ICacheAccess.class, TileJobOptions.class)
                .newInstance(this.testLayer, MVTLayer.getCache(), new TileJobOptions(50, 50, Collections.emptyMap(), 1))
                .createTileLoaderJob(mvtTile).submit();
        Awaitility.await().atMost(Durations.FIVE_SECONDS).until(() -> finishedLoading.finished);
        assertFalse(this.testLayer.getData().allPrimitives().isEmpty());
    }

    /**
     * For some reason, lambdas get garbage collected by WeakReference's. This avoids that.
     */
    private static final class FinishedLoading implements MVTTile.TileListener {
        boolean finished;
        @Override
        public void finishedLoading(MVTTile tile) {
            this.finished = true;
        }
    }
}
