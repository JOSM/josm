// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.gui.jmapviewer.Tile;
import org.openstreetmap.gui.jmapviewer.TileXY;
import org.openstreetmap.gui.jmapviewer.tilesources.TemplatedTMSTileSource;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.Projections;

public class TemplatedWMSTileSourceTest {

    private ImageryInfo testImageryWMS =  new ImageryInfo("test imagery", "http://localhost", "wms", null, null);
    private ImageryInfo testImageryTMS =  new ImageryInfo("test imagery", "http://localhost", "tms", null, null);
    //private TileSource testSource = new TemplatedWMSTileSource(testImageryWMS);
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
        verifyMercatorTile(source, 0, 1, 2);
        verifyMercatorTile(source, 0, 0, 0);
        verifyMercatorTile(source, 0, 0, 1);
        verifyMercatorTile(source, 0, 1, 1);
        verifyMercatorTile(source, 1, 0, 1);
        verifyMercatorTile(source, 1, 1, 1);
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                verifyMercatorTile(source, x, y, 2);
                verifyTileSquarness(source, x, y, 2);
            }
        }
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
    }

    @Test
    public void testEPSG2180() {
        Main.setProjection(Projections.getProjectionByCode("EPSG:2180"));
        TemplatedWMSTileSource source = getSource();

        verifyLocation(source, new LatLon(53.5937132, 19.5652017));
        verifyLocation(source, new LatLon(53.501565692302854, 18.54455233898721));

        verifyTileSquarness(source, 2, 2, 2);
        verifyTileSquarness(source, 150, 20, 18);

    }

    private void verifyMercatorTile(TemplatedWMSTileSource source, int x, int y, int z) {
        TemplatedTMSTileSource verifier = new TemplatedTMSTileSource(testImageryTMS);
        LatLon result = getTileLatLon(source, x, y, z);
        LatLon expected = new LatLon(verifier.tileYToLat(y, z), verifier.tileXToLon(x, z)); //
        System.out.println(z + "/" + x + "/" + y + " - result: " + result.toDisplayString() + " osmMercator: " +  expected.toDisplayString());
        assertTrue("result: " + result.toDisplayString() + " osmMercator: " +  expected.toDisplayString(), result.equalsEpsilon(expected));
        LatLon tileCenter = new Bounds(result, getTileLatLon(source, x+1, y+1, z)).getCenter();
        TileXY backwardsResult = source.latLonToTileXY(tileCenter.toCoordinate(), z);
        assertEquals(x, backwardsResult.getXIndex());
        assertEquals(y, backwardsResult.getYIndex());
    }

    private void verifyLocation(TemplatedWMSTileSource source, LatLon location) {
        for (int z = 1; z < 22; z++) {
            TileXY tileIndex = source.latLonToTileXY(location.toCoordinate(), z);
            BBox bbox = new BBox(
                    getTileLatLon(source, tileIndex, z),
                    getTileLatLon(source, tileIndex.getXIndex() + 1, tileIndex.getYIndex() + 1, z)
                    );
            assertTrue(location.toDisplayString() + " not within " + bbox.toString() +
                    " for tile " + z + "/" + tileIndex.getXIndex() + "/" + tileIndex.getYIndex(),
                    bbox.bounds(location));
        }
    }

    private LatLon getTileLatLon(TemplatedWMSTileSource source, TileXY tileIndex, int z) {
        return getTileLatLon(source, tileIndex.getXIndex(), tileIndex.getYIndex(), z);
    }

    private LatLon getTileLatLon(TemplatedWMSTileSource source, int x, int y, int z) {
        return new LatLon(source.tileXYToLatLon(new Tile(source, x, y, z)));
    }

    private void verifyTileSquarness(TemplatedWMSTileSource source, int x, int y, int z) {
        Projection proj = Main.getProjection();
        EastNorth min = proj.latlon2eastNorth(getTileLatLon(source, x, y, z));
        EastNorth max = proj.latlon2eastNorth(getTileLatLon(source, x + 1, y + 1, z));
        double y_size = Math.abs(min.getY() - max.getY());
        double x_size = Math.abs(min.getX() - max.getX());
        assertEquals(x_size, y_size, 1e-05);
    }

    private TemplatedWMSTileSource getSource() {
        TemplatedWMSTileSource source = new TemplatedWMSTileSource(testImageryWMS);
        return source;
    }
}
