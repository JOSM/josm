// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.gui.jmapviewer.TileXY;
import org.openstreetmap.gui.jmapviewer.tilesources.TemplatedTMSTileSource;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.Projections;

public class WMTSTileSourceTest {

    private ImageryInfo testImageryTMS =  new ImageryInfo("test imagery", "http://localhost", "tms", null, null);
    private ImageryInfo testImageryPSEUDO_MERCATOR = getImagery("test/data/wmts/getcapabilities-pseudo-mercator.xml");
    private ImageryInfo testImageryTOPO_PL = getImagery("test/data/wmts/getcapabilities-TOPO.xml");
    private ImageryInfo testImageryORTO_PL = getImagery("test/data/wmts/getcapabilities-ORTO.xml");
    private ImageryInfo testImageryWIEN = getImagery("test/data/wmts/getCapabilities-wien.xml");
    private ImageryInfo testImageryWALLONIE = getImagery("test/data/wmts/WMTSCapabilities-Wallonie.xml");

    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    private static ImageryInfo getImagery(String path) {
        try {
            return new ImageryInfo(
                    "test",
                    new File(path).toURI().toURL().toString()
                    );
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Test
    public void testPseudoMercator() throws MalformedURLException, IOException {
        Main.setProjection(Projections.getProjectionByCode("EPSG:3857"));
        WMTSTileSource testSource = new WMTSTileSource(testImageryPSEUDO_MERCATOR);

        verifyMercatorTile(testSource, 0, 0, 1);
        verifyMercatorTile(testSource, 0, 0, 2);
        verifyMercatorTile(testSource, 1, 1, 2);
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                verifyMercatorTile(testSource, x, y, 3);
            }
        }
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 4; y++) {
                verifyMercatorTile(testSource, x, y, 4);
            }
        }

        verifyMercatorTile(testSource, 2 << 9 - 1, 2 << 8 - 1, 10);

        assertEquals("TileXMax", 1, testSource.getTileXMax(1));
        assertEquals("TileYMax", 1, testSource.getTileYMax(1));
        assertEquals("TileXMax", 2, testSource.getTileXMax(2));
        assertEquals("TileYMax", 2, testSource.getTileYMax(2));
        assertEquals("TileXMax", 5, testSource.getTileXMax(3));
        assertEquals("TileYMax", 4, testSource.getTileYMax(3));

    }

    @Test
    public void testWALLONIE() throws MalformedURLException, IOException {
        Main.setProjection(Projections.getProjectionByCode("EPSG:31370"));
        WMTSTileSource testSource = new WMTSTileSource(testImageryWALLONIE);
        Bounds wallonieBounds = new Bounds(
                new LatLon(49.485372459967245, 2.840548314430268),
                new LatLon(50.820959517561256, 6.427849693016202)
                );
        verifyBounds(wallonieBounds, testSource, 10, 20324, 17724);

    }

    private void verifyBounds(Bounds bounds, WMTSTileSource testSource, int z, int x, int y) {
        LatLon ret = new LatLon(testSource.tileXYToLatLon(y, y, z));
        assertTrue(ret.toDisplayString() + " doesn't lie within: " + bounds.toString(), bounds.contains(ret));
    }

    @Test
    public void testWIEN() throws MalformedURLException, IOException {
        Main.setProjection(Projections.getProjectionByCode("EPSG:3857"));
        WMTSTileSource testSource = new WMTSTileSource(testImageryWIEN);
        int zoomOffset = 10;

        // Linz - 11/1105/709.png
        verifyMercatorTile(testSource, 0, 0, 1, zoomOffset);
        verifyMercatorTile(testSource, 1105, 709, 2, zoomOffset);
        verifyMercatorTile(testSource, 1, 1, 1, zoomOffset);
        verifyMercatorTile(testSource, 2, 2, 1, zoomOffset);
        verifyMercatorTile(testSource, 0, 0, 2, zoomOffset);
        verifyMercatorTile(testSource, 1, 1, 2, zoomOffset);


        LatLon ll = new LatLon(testSource.tileXYToLatLon(500,  500, 1));

        TileXY xy = testSource.latLonToTileXY(new LatLon(48.21, 14.24).toCoordinate(), 1);
        assertTrue("X index is negative: " + xy.getXIndex(), xy.getXIndex() > 0);
        assertTrue(xy.getYIndex() > 0);
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                verifyMercatorTile(testSource, x, y, 3, zoomOffset);
            }
        }
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 4; y++) {
                verifyMercatorTile(testSource, x, y, zoomOffset);
            }
        }

        verifyMercatorTile(testSource, 2 << 9 - 1, 2 << 8 - 1, zoomOffset);

        assertEquals("TileXMax", 1, testSource.getTileXMax(1));
        assertEquals("TileYMax", 1, testSource.getTileYMax(1));
        assertEquals("TileXMax", 2, testSource.getTileXMax(2));
        assertEquals("TileYMax", 2, testSource.getTileYMax(2));
        assertEquals("TileXMax", 4, testSource.getTileXMax(3));
        assertEquals("TileYMax", 4, testSource.getTileYMax(3));
    }

    @Test
    public void testGeoportalTOPOPL() throws IOException {
        Main.setProjection(Projections.getProjectionByCode("EPSG:4326"));
        WMTSTileSource testSource = new WMTSTileSource(testImageryTOPO_PL);
        verifyTile(new LatLon(56, 12), testSource, 0, 0, 1);
        verifyTile(new LatLon(56, 12), testSource, 0, 0, 2);
        verifyTile(new LatLon(51.1268639, 16.8731360), testSource, 1, 1, 2);

        assertEquals("TileXMax", 37, testSource.getTileXMax(1));
        assertEquals("TileYMax", 19, testSource.getTileYMax(1));
        assertEquals("TileXMax", 74, testSource.getTileXMax(2));
        assertEquals("TileYMax", 37, testSource.getTileYMax(2));
        assertEquals("TileXMax", 148, testSource.getTileXMax(3));
        assertEquals("TileYMax", 74, testSource.getTileYMax(3));
        assertEquals(
                "http://mapy.geoportal.gov.pl/wss/service/WMTS/guest/wmts/TOPO?SERVICE=WMTS&REQUEST=GetTile&"
                + "VERSION=1.0.0&LAYER=MAPA TOPOGRAFICZNA&STYLE=&FORMAT=image/jpeg&tileMatrixSet=EPSG:4326&"
                + "tileMatrix=EPSG:4326:0&tileRow=1&tileCol=1",
                testSource.getTileUrl(1,  1,  1));
    }

    @Test
    public void testGeoportalORTOPL4326() throws IOException {
        Main.setProjection(Projections.getProjectionByCode("EPSG:4326"));
        WMTSTileSource testSource = new WMTSTileSource(testImageryORTO_PL);
        verifyTile(new LatLon(53.5993712684958, 19.560669777688176), testSource, 12412, 3941, 14);

        verifyTile(new LatLon(49.783096954497786, 22.79034127751704), testSource, 17714, 10206, 14);
    }

    @Test
    public void testGeoportalORTOPL2180() throws IOException {
        Main.setProjection(Projections.getProjectionByCode("EPSG:2180"));
        WMTSTileSource testSource = new WMTSTileSource(testImageryORTO_PL);

        verifyTile(new LatLon(53.59940948387726, 19.560544913270064), testSource, 6453, 3140, 14);
        verifyTile(new LatLon(49.782984840526055, 22.790064966993445), testSource, 9932, 9305, 14);
    }

    private void verifyTile(LatLon expected, WMTSTileSource source, int x, int y, int z) {
        LatLon ll = new LatLon(source.tileXYToLatLon(x, y, z));
        assertEquals("Latitude", expected.lat(), ll.lat(), 1e-05);
        assertEquals("Longitude", expected.lon(), ll.lon(), 1e-05);

    }

    private void verifyMercatorTile(WMTSTileSource testSource, int x, int y, int z) {
        verifyMercatorTile(testSource, x, y, z, -1);
    }

    private void verifyMercatorTile(WMTSTileSource testSource, int x, int y, int z, int zoomOffset) {
        TemplatedTMSTileSource verifier = new TemplatedTMSTileSource(testImageryTMS);
        LatLon result = new LatLon(testSource.tileXYToLatLon(x, y, z));
        LatLon expected = new LatLon(verifier.tileXYToLatLon(x, y, z + zoomOffset));
        System.out.println(z + "/" + x + "/" + y + " - result: " + result.toDisplayString() + " osmMercator: " +  expected.toDisplayString());
        assertEquals("Longitude" , expected.lon(), result.lon(), 1e-04);
        assertEquals("Latitude", expected.lat(), result.lat(), 1e-04);
        //assertTrue("result: " + result.toDisplayString() + " osmMercator: " +  expected.toDisplayString(), result.equalsEpsilon(expected));
//        LatLon tileCenter = new Bounds(result, new LatLon(testSource.tileXYToLatLon(x+1, y+1, z))).getCenter();
//        TileXY backwardsResult = testSource.latLonToTileXY(tileCenter.toCoordinate(), z);
        //assertEquals(x, backwardsResult.getXIndex());
        //assertEquals(y, backwardsResult.getYIndex());
    }
}
