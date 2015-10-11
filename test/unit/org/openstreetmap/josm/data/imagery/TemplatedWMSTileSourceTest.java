// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.gui.jmapviewer.TileXY;
import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;
import org.openstreetmap.gui.jmapviewer.tilesources.TemplatedTMSTileSource;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.projection.CustomProjection;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.Projections;

/**
 * Unit tests for class {@link TemplatedWMSTileSource}.
 */
public class TemplatedWMSTileSourceTest {

    private ImageryInfo testImageryWMS =  new ImageryInfo("test imagery", "http://localhost", "wms", null, null);
    private ImageryInfo testImageryTMS =  new ImageryInfo("test imagery", "http://localhost", "tms", null, null);

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    @Test
    public void testEPSG3857() {
        Main.setProjection(Projections.getProjectionByCode("EPSG:3857"));
        TemplatedWMSTileSource source = new TemplatedWMSTileSource(testImageryWMS);
        verifyMercatorTile(source, 0, 0, 1);
        verifyMercatorTile(source, 0, 0, 2);
        verifyMercatorTile(source, 0, 1, 2);
        verifyMercatorTile(source, 1, 0, 2);
        verifyMercatorTile(source, 1, 1, 2);
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                verifyMercatorTile(source, x, y, 3);
                verifyTileSquarness(source, x, y, 3);
            }
        }
        verifyTileSquarness(source, 150, 20, 18);
        verifyTileSquarness(source, 2270, 1323, 12);
        verifyLocation(source, new LatLon(53.5937132, 19.5652017));
        verifyLocation(source, new LatLon(53.501565692302854, 18.54455233898721));
    }

    @Test
    public void testEPSG4326() {
        Main.setProjection(Projections.getProjectionByCode("EPSG:4326"));
        TemplatedWMSTileSource source = getSource();

        verifyLocation(source, new LatLon(53.5937132, 19.5652017));
        verifyLocation(source, new LatLon(53.501565692302854, 18.54455233898721));
        verifyTileSquarness(source, 2, 2, 2);
        verifyTileSquarness(source, 150, 20, 18);
        verifyTileSquarness(source, 2270, 1323, 12);
    }

    @Test
    public void testEPSG4326_widebounds() {
        Main.setProjection(new CustomProjection("+proj=lonlat +datum=WGS84 +axis=neu +bounds=-180,53,180,54"));
        TemplatedWMSTileSource source = getSource();

        verifyLocation(source, new LatLon(53.5937132, 19.5652017));
        verifyLocation(source, new LatLon(53.501565692302854, 18.54455233898721));
    }

    @Test
    public void testEPSG4326_narrowbounds() {
        Main.setProjection(new CustomProjection("+proj=lonlat +datum=WGS84 +axis=neu +bounds=18,-90,20,90"));
        TemplatedWMSTileSource source = getSource();

        verifyLocation(source, new LatLon(53.5937132, 19.5652017));
        verifyLocation(source, new LatLon(53.501565692302854, 18.54455233898721));
    }

    @Test
    public void testEPSG2180() {
        Main.setProjection(Projections.getProjectionByCode("EPSG:2180"));
        TemplatedWMSTileSource source = getSource();

        verifyLocation(source, new LatLon(53.5937132, 19.5652017));
        verifyLocation(source, new LatLon(53.501565692302854, 18.54455233898721));

        verifyTileSquarness(source, 2, 2, 2);
        verifyTileSquarness(source, 150, 20, 18);
        verifyTileSquarness(source, 2270, 1323, 12);
    }

    @Test
    public void testEPSG3006_withbounds() {
        Main.setProjection(
                new CustomProjection("+proj=utm +zone=33 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 "
                        + "+units=m +no_defs +axis=neu +wmssrs=EPSG:3006 +bounds=10.5700,55.2000,24.1800,69.1000 "));
        TemplatedWMSTileSource source = getSource();

        verifyLocation(source, new LatLon(60, 18), 3);
        verifyLocation(source, new LatLon(60, 18));
    }

    @Test
    public void testEPSG3006_withoutbounds() {
        Main.setProjection(
                new CustomProjection("+proj=utm +zone=33 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 "
                        + "+units=m +no_defs +axis=neu +wmssrs=EPSG:3006"));
        TemplatedWMSTileSource source = getSource();

        verifyTileSquarness(source, 0, 1, 4);
        verifyLocation(source, new LatLon(60, 18.1), 3);
        verifyLocation(source, new LatLon(60, 18.1));
    }

    private void verifyMercatorTile(TemplatedWMSTileSource source, int x, int y, int z) {
        TemplatedTMSTileSource verifier = new TemplatedTMSTileSource(testImageryTMS);
        LatLon result = getTileLatLon(source, x, y, z);
        ICoordinate expected = verifier.tileXYToLatLon(x, y, z - 1);
        assertEquals(expected.getLat(), result.lat(), 1e-4);
        assertEquals(expected.getLon(), result.lon(), 1e-4);
        LatLon tileCenter = new Bounds(result, getTileLatLon(source, x+1, y+1, z)).getCenter();
        TileXY backwardsResult = source.latLonToTileXY(tileCenter.toCoordinate(), z);
        assertEquals(x, backwardsResult.getXIndex());
        assertEquals(y, backwardsResult.getYIndex());
    }

    private void verifyLocation(TemplatedWMSTileSource source, LatLon location) {
        for (int z = source.getMaxZoom(); z > source.getMinZoom() + 1; z--) {
            if (source.getTileXMax(z) != source.getTileXMin(z) && source.getTileYMax(z) != source.getTileYMin(z)) {
                // do the tests only where there is more than one tile
                verifyLocation(source, location, z);
            }
        }
    }

    private void verifyLocation(TemplatedWMSTileSource source, LatLon location, int z) {
        assertTrue(
                "Point outside world bounds",
                Main.getProjection().getWorldBoundsLatLon().contains(location)
                );

        TileXY tileIndex = source.latLonToTileXY(location.toCoordinate(), z);

        assertTrue("X index: " + tileIndex.getXIndex() + " greater than tileXmax: " + source.getTileXMax(z) + " at zoom: " + z,
                tileIndex.getXIndex() <= source.getTileXMax(z));

        assertTrue("Y index: " + tileIndex.getYIndex() + " greater than tileYmax: " + source.getTileYMax(z) + " at zoom: " + z,
                tileIndex.getYIndex() <= source.getTileYMax(z));

        // test that location is within tile bounds
        BBox bbox = new BBox(
                getTileLatLon(source, tileIndex, z),
                getTileLatLon(source, tileIndex.getXIndex() + 1, tileIndex.getYIndex() + 1, z)
                );
        assertTrue(location.toDisplayString() + " not within " + bbox.toString() +
                " for tile " + z + "/" + tileIndex.getXIndex() + "/" + tileIndex.getYIndex(),
                bbox.bounds(location));
        verifyTileSquarness(source, tileIndex.getXIndex(), tileIndex.getYIndex(), z);
    }

    private LatLon getTileLatLon(TemplatedWMSTileSource source, TileXY tileIndex, int z) {
        return getTileLatLon(source, tileIndex.getXIndex(), tileIndex.getYIndex(), z);
    }

    private LatLon getTileLatLon(TemplatedWMSTileSource source, int x, int y, int z) {
        return new LatLon(source.tileXYToLatLon(x, y, z));
    }

    private void verifyTileSquarness(TemplatedWMSTileSource source, int x, int y, int z) {
        Projection proj = Main.getProjection();
        /**
         * t1 | t2
         * -------
         * t3 | t4
         */
        EastNorth t1 = source.getTileEastNorth(x, y, z);
        EastNorth t2 = source.getTileEastNorth(x + 1, y, z);
        EastNorth t3 = source.getTileEastNorth(x, y + 1, z);
        EastNorth t4 = source.getTileEastNorth(x + 1, y + 1, z);
        double y_size = Math.abs(t1.getY() - t4.getY());
        double x_size = Math.abs(t1.getX() - t4.getX());

        assertEquals(x_size, y_size, Math.max(x_size, y_size) * 1e-06);
        assertEquals(y_size, Math.abs(t1.getY() - t3.getY()), y_size * 1e-06);
        assertEquals(x_size, Math.abs(t1.getX() - t2.getX()), x_size * 1e-06);

        t1 = source.getTileEastNorth(x, y, z);
        t2 = source.getTileEastNorth(x + 1, y, z);
        t3 = source.getTileEastNorth(x, y + 1, z);
        t4 = source.getTileEastNorth(x + 1, y + 1, z);
        y_size = Math.abs(t1.getY() - t4.getY());
        x_size = Math.abs(t1.getX() - t4.getX());
        assertEquals(x_size, y_size, Math.max(x_size, y_size) * 1e-05);
        assertEquals(y_size, Math.abs(t1.getY() - t3.getY()), y_size * 1e-05);
        assertEquals(x_size, Math.abs(t1.getX() - t2.getX()), x_size * 1e-05);
    }

    private TemplatedWMSTileSource getSource() {
        TemplatedWMSTileSource source = new TemplatedWMSTileSource(testImageryWMS);
        return source;
    }
}
