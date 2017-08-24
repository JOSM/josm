// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.Point;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.Projected;
import org.openstreetmap.gui.jmapviewer.Tile;
import org.openstreetmap.gui.jmapviewer.TileRange;
import org.openstreetmap.gui.jmapviewer.TileXY;
import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;
import org.openstreetmap.gui.jmapviewer.interfaces.IProjected;
import org.openstreetmap.gui.jmapviewer.interfaces.TileCache;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoader;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoaderListener;
import org.openstreetmap.gui.jmapviewer.tilesources.AbstractTMSTileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.TileSourceInfo;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.TileLoaderFactory;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.imagery.ImageryFilterSettings;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Test of the base {@link AbstractTileSourceLayer} class
 */
public class AbstractTileSourceLayerTest {

    /**
     * Setup test
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().platform().projection().main();

    private static final class TMSTileStubSource extends AbstractTMSTileSource {
        private TMSTileStubSource() {
            super(new TileSourceInfo());
        }

        @Override
        public double getDistance(double la1, double lo1, double la2, double lo2) {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public Point latLonToXY(double lat, double lon, int zoom) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ICoordinate xyToLatLon(int x, int y, int zoom) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public TileXY latLonToTileXY(double lat, double lon, int zoom) {
            return new TileXY(lon / 2, lat / 2);
        }

        @Override
        public ICoordinate tileXYToLatLon(int x, int y, int zoom) {
            return new Coordinate(2*y, 2*x);
        }

        @Override
        public IProjected tileXYtoProjected(int x, int y, int zoom) {
            return new Projected(2*x, 2*y);
        }

        @Override
        public TileXY projectedToTileXY(IProjected p, int zoom) {
            return new TileXY(p.getEast() / 2, p.getNorth() / 2);
        }

        @Override
        public boolean isInside(Tile inner, Tile outer) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public TileRange getCoveringTileRange(Tile tile, int newZoom) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getServerCRS() {
            return "EPSG:3857";
        }
    }

    private static class TileSourceStubLayer extends AbstractTileSourceLayer<AbstractTMSTileSource> {

        TileSourceStubLayer() {
            super(new ImageryInfo());
            hookUpMapView();
        }

        @Override
        protected TileLoaderFactory getTileLoaderFactory() {
            return new TileLoaderFactory() {
                @Override
                public TileLoader makeTileLoader(TileLoaderListener listener, Map<String, String> headers) {
                    return null;
                }
            };
        }

        @Override
        public Collection<String> getNativeProjections() {
            return null;
        }

        @Override
        protected AbstractTMSTileSource getTileSource() {
            return new TMSTileStubSource();
        }

        TileCache getTileCache() {
            return tileCache;
        }
    }

    private TileSourceStubLayer testLayer;
    AtomicBoolean invalidated = new AtomicBoolean();

    /**
     * Create test layer
     */
    @Before
    public void setUp() {
        Main.getLayerManager().addLayer(new OsmDataLayer(new DataSet(), "", null));
        testLayer = new TileSourceStubLayer();
        testLayer.addInvalidationListener(l -> invalidated.set(true));
    }

    /**
     * Test {@link AbstractTileSourceLayer#filterChanged}
     */
    @Test
    public void testFilterChanged() {
        try {
            ImageryFilterSettings filterSettings = new ImageryFilterSettings();
            filterSettings.addFilterChangeListener(testLayer);
            assertFalse(invalidated.get());
            filterSettings.setGamma(0.5);
            assertTrue(invalidated.get());
        } finally {
            invalidated.set(false);
        }
    }

    /**
     * Test {@link AbstractTileSourceLayer#clearTileCache}
     */
    @Test
    public void testClearTileCache() {
        testLayer.loadAllTiles(true);
        assertTrue(testLayer.getTileCache().getTileCount() > 0);
        testLayer.clearTileCache();
        assertEquals(0, testLayer.getTileCache().getTileCount());
    }

    /**
     * Test {@link AbstractTileSourceLayer#getAdjustAction}
     */
    @Test
    public void testGetAdjustAction() {
        assertNotNull(testLayer.getAdjustAction());
    }

    /**
     * Test {@link AbstractTileSourceLayer#getInfoComponent}
     */
    @Test
    public void testGetInfoComponent() {
        assertNotNull(testLayer.getInfoComponent());
    }

    /**
     * Test {@link AbstractTileSourceLayer.TileSourceLayerPopup}
     */
    @Test
    public void testTileSourceLayerPopup() {
        assertNotNull(testLayer.new TileSourceLayerPopup());
    }
}
