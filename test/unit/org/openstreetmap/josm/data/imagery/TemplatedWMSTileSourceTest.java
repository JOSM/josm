// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.gui.jmapviewer.TileXY;
import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;
import org.openstreetmap.gui.jmapviewer.tilesources.TemplatedTMSTileSource;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.CustomProjection;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link TemplatedWMSTileSource}.
 */
public class TemplatedWMSTileSourceTest {

    private final ImageryInfo testImageryWMS = new ImageryInfo("test imagery", "http://localhost", "wms", null, null);
    private final ImageryInfo testImageryTMS = new ImageryInfo("test imagery", "http://localhost", "tms", null, null);

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Test EPSG:3857
     */
    @Test
    public void testEPSG3857() {
        Projection projection = Projections.getProjectionByCode("EPSG:3857");
        ProjectionRegistry.setProjection(projection);
        TemplatedWMSTileSource source = new TemplatedWMSTileSource(testImageryWMS, projection);
        verifyMercatorTile(source, 0, 0, 1);
        verifyMercatorTile(source, 0, 0, 2);
        verifyMercatorTile(source, 0, 1, 2);
        verifyMercatorTile(source, 1, 0, 2);
        verifyMercatorTile(source, 1, 1, 2);
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                verifyMercatorTile(source, x, y, 3);
                verifyTileSquareness(source, x, y, 3);
            }
        }
        verifyTileSquareness(source, 150, 20, 18);
        verifyTileSquareness(source, 2270, 1323, 12);
        verifyLocation(source, new LatLon(53.5937132, 19.5652017));
        verifyLocation(source, new LatLon(53.501565692302854, 18.54455233898721));
    }

    /**
     * Test EPSG:4326
     */
    @Test
    public void testEPSG4326() {
        Projection projection = Projections.getProjectionByCode("EPSG:4326");
        ProjectionRegistry.setProjection(projection);
        TemplatedWMSTileSource source = getSource(projection);

        verifyLocation(source, new LatLon(53.5937132, 19.5652017));
        verifyLocation(source, new LatLon(53.501565692302854, 18.54455233898721));
        verifyTileSquareness(source, 2, 2, 2);
        verifyTileSquareness(source, 150, 20, 18);
        verifyTileSquareness(source, 2270, 1323, 12);
    }

    /**
     * Test EPSG:4326 - wide bounds
     */
    @Test
    public void testEPSG4326widebounds() {
        Projection projection = new CustomProjection("+proj=lonlat +datum=WGS84 +axis=neu +bounds=-180,53,180,54");
        ProjectionRegistry.setProjection(projection);
        TemplatedWMSTileSource source = getSource(projection);

        verifyLocation(source, new LatLon(53.5937132, 19.5652017));
        verifyLocation(source, new LatLon(53.501565692302854, 18.54455233898721));
    }

    /**
     * Test EPSG:4326 - narrow bounds
     */
    @Test
    public void testEPSG4326narrowbounds() {
        Projection projection = new CustomProjection("+proj=lonlat +datum=WGS84 +axis=neu +bounds=18,-90,20,90");
        ProjectionRegistry.setProjection(projection);
        TemplatedWMSTileSource source = getSource(projection);

        verifyLocation(source, new LatLon(53.5937132, 19.5652017));
        verifyLocation(source, new LatLon(53.501565692302854, 18.54455233898721));
    }

    /**
     * Test EPSG:2180
     */
    @Test
    public void testEPSG2180() {
        Projection projection = Projections.getProjectionByCode("EPSG:2180");
        ProjectionRegistry.setProjection(projection);
        TemplatedWMSTileSource source = getSource(projection);

        verifyLocation(source, new LatLon(53.5937132, 19.5652017));
        verifyLocation(source, new LatLon(53.501565692302854, 18.54455233898721));

        verifyTileSquareness(source, 2, 2, 2);
        verifyTileSquareness(source, 150, 20, 18);
        verifyTileSquareness(source, 2270, 1323, 12);
    }

    /**
     * Test EPSG:3006 with bounds
     */
    @Test
    public void testEPSG3006withbounds() {
        Projection projection =
                new CustomProjection("+proj=utm +zone=33 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 "
                        + "+units=m +no_defs +axis=neu +wmssrs=EPSG:3006 +bounds=10.5700,55.2000,24.1800,69.1000 ");
        ProjectionRegistry.setProjection(projection);
        TemplatedWMSTileSource source = getSource(projection);

        verifyTileSquareness(source, 0, 1, 4);
        verifyLocation(source, new LatLon(60, 18.1), 3);
        verifyLocation(source, new LatLon(60, 18.1));
    }

    /**
     * Test EPSG:3006 without bounds
     */
    @Test
    public void testEPSG3006withoutbounds() {
        Projection projection =
                new CustomProjection("+proj=utm +zone=33 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 "
                        + "+units=m +no_defs +axis=neu +wmssrs=EPSG:3006");
        ProjectionRegistry.setProjection(projection);
        TemplatedWMSTileSource source = getSource(projection);

        verifyTileSquareness(source, 0, 1, 4);
        verifyLocation(source, new LatLon(60, 18.1), 3);
        verifyLocation(source, new LatLon(60, 18.1));
    }

    /**
     * Test getTileUrl
     */
    @Test
    public void testGetTileUrl() {
        // "https://maps.six.nsw.gov.au/arcgis/services/public/NSW_Imagery_Dates/MapServer/WMSServer?
        // SERVICE=WMS&VERSION=1.3.0&REQUEST=GetMap&CRS={proj}&BBOX={bbox}&WIDTH={width}&HEIGHT={height}
        // &LAYERS=0&STYLES=&FORMAT=image/png32&DPI=96&MAP_RESOLUTION=96&FORMAT_OPTIONS=dpi:96&TRANSPARENT=TRUE",
        Projection projection = Projections.getProjectionByCode("EPSG:4326");
        ProjectionRegistry.setProjection(projection);
        ImageryInfo testImageryWMS = new ImageryInfo("test imagery",
                "https://maps.six.nsw.gov.au/arcgis/services/public/NSW_Imagery_Dates/MapServer/WMSServer?SERVICE=WMS&VERSION=1.3.0&"
                + "REQUEST=GetMap&CRS={proj}&BBOX={bbox}&WIDTH={width}&HEIGHT={height}&LAYERS=0&STYLES=&FORMAT=image/png32&DPI=96&"
                + "MAP_RESOLUTION=96&FORMAT_OPTIONS=dpi:96&TRANSPARENT=TRUE",
                "wms",
                null,
                null);
        TemplatedWMSTileSource ts = new TemplatedWMSTileSource(testImageryWMS, projection);
        assertEquals("https://maps.six.nsw.gov.au/arcgis/services/public/NSW_Imagery_Dates/MapServer/WMSServer?SERVICE=WMS&"
                + "VERSION=1.3.0&REQUEST=GetMap&CRS=EPSG:4326&BBOX=-1349.9999381,539.9999691,-989.9999536,899.9999536&WIDTH=512&"
                + "HEIGHT=512&LAYERS=0&STYLES=&FORMAT=image/png32&DPI=96&MAP_RESOLUTION=96&FORMAT_OPTIONS=dpi:96&TRANSPARENT=TRUE",
                ts.getTileUrl(1, 2, 3));
        assertEquals("https://maps.six.nsw.gov.au/arcgis/services/public/NSW_Imagery_Dates/MapServer/WMSServer?SERVICE=WMS&"
                + "VERSION=1.3.0&REQUEST=GetMap&CRS=EPSG:4326&BBOX=-89.9999923,-0.0000077,0.0000039,89.9999884&WIDTH=512&HEIGHT=512&"
                + "LAYERS=0&STYLES=&FORMAT=image/png32&DPI=96&MAP_RESOLUTION=96&FORMAT_OPTIONS=dpi:96&TRANSPARENT=TRUE",
                ts.getTileUrl(3, 2, 1));
        testImageryWMS = new ImageryInfo("test imagery",
                "https://services.slip.wa.gov.au/public/services/SLIP_Public_Services/Transport/MapServer/WMSServer?LAYERS=8&"
                + "TRANSPARENT=TRUE&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&STYLES=&FORMAT=image%2Fpng&SRS={proj}&BBOX={bbox}&"
                + "WIDTH={width}&HEIGHT={height}",
                "wms",
                null,
                null);
        ts = new TemplatedWMSTileSource(testImageryWMS, projection);
        assertEquals("https://services.slip.wa.gov.au/public/services/SLIP_Public_Services/Transport/MapServer/WMSServer?LAYERS=8&"
                + "TRANSPARENT=TRUE&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&STYLES=&FORMAT=image%2Fpng&SRS=EPSG:4326&"
                + "BBOX=539.9999691,-1349.9999381,899.9999536,-989.9999536&WIDTH=512&HEIGHT=512",
                ts.getTileUrl(1, 2, 3));
        assertEquals("https://services.slip.wa.gov.au/public/services/SLIP_Public_Services/Transport/MapServer/WMSServer?LAYERS=8&"
                + "TRANSPARENT=TRUE&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&STYLES=&FORMAT=image%2Fpng&SRS=EPSG:4326&"
                + "BBOX=-0.0000077,-89.9999923,89.9999884,0.0000039&WIDTH=512&HEIGHT=512", ts.getTileUrl(3, 2, 1));
    }

    private void verifyMercatorTile(TemplatedWMSTileSource source, int x, int y, int z) {
        TemplatedTMSTileSource verifier = new TemplatedTMSTileSource(testImageryTMS);
        LatLon result = getTileLatLon(source, x, y, z);
        ICoordinate expected = verifier.tileXYToLatLon(x, y, z - 1);
        assertEquals(expected.getLat(), result.lat(), 1e-4);
        assertEquals(LatLon.normalizeLon(expected.getLon() - result.lon()), 0.0, 1e-4);
        LatLon tileCenter = new Bounds(result, getTileLatLon(source, x+1, y+1, z)).getCenter();
        TileXY backwardsResult = source.latLonToTileXY(CoordinateConversion.llToCoor(tileCenter), z);
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
        Projection projection = ProjectionRegistry.getProjection();
        assertTrue(
                "Point outside world bounds",
                projection.getWorldBoundsLatLon().contains(location)
                );

        TileXY tileIndex = source.latLonToTileXY(CoordinateConversion.llToCoor(location), z);

        assertTrue("X index: " + tileIndex.getXIndex() + " greater than tileXmax: " + source.getTileXMax(z) + " at zoom: " + z,
                tileIndex.getXIndex() <= source.getTileXMax(z));

        assertTrue("Y index: " + tileIndex.getYIndex() + " greater than tileYmax: " + source.getTileYMax(z) + " at zoom: " + z,
                tileIndex.getYIndex() <= source.getTileYMax(z));

        EastNorth locationEN = projection.latlon2eastNorth(location);
        EastNorth x1 = projection.latlon2eastNorth(getTileLatLon(source, tileIndex, z));
        EastNorth x2 = projection.latlon2eastNorth(getTileLatLon(source, tileIndex.getXIndex() + 1, tileIndex.getYIndex() + 1, z));
        // test that location is within tile bounds
        assertTrue(locationEN.toString() + " not within " + bboxStr(x1, x2) +
                " for tile " + z + "/" + tileIndex.getXIndex() + "/" + tileIndex.getYIndex(),
                isWithin(locationEN, x1, x2));
        verifyTileSquareness(source, tileIndex.getXIndex(), tileIndex.getYIndex(), z);
    }

    private static boolean isWithin(EastNorth point, EastNorth topLeft, EastNorth bottomRight) {
        return Math.min(topLeft.east(), bottomRight.east()) <= point.east() &&
                point.east() <= Math.max(topLeft.east(), bottomRight.east()) &&
                Math.min(topLeft.north(), bottomRight.north()) <= point.north() &&
                point.north() <= Math.max(topLeft.north(), bottomRight.north());
    }

    private static String bboxStr(EastNorth x1, EastNorth x2) {
        return "[" + x1.east() +", " + x1.north() + ", " + x2.east() + ", " + x2.north() +"]";
    }

    private LatLon getTileLatLon(TemplatedWMSTileSource source, TileXY tileIndex, int z) {
        return getTileLatLon(source, tileIndex.getXIndex(), tileIndex.getYIndex(), z);
    }

    private LatLon getTileLatLon(TemplatedWMSTileSource source, int x, int y, int z) {
        return CoordinateConversion.coorToLL(source.tileXYToLatLon(x, y, z));
    }

    private void verifyTileSquareness(TemplatedWMSTileSource source, int x, int y, int z) {
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

    private TemplatedWMSTileSource getSource(Projection projection) {
        return new TemplatedWMSTileSource(testImageryWMS, projection);
    }
}
