// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.CachedLatLon;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.io.GpxReaderTest;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.spi.preferences.IPreferences;
import org.openstreetmap.josm.testutils.annotations.Timezone;
import org.openstreetmap.josm.tools.date.DateUtils;
import org.xml.sax.SAXException;

/**
 * Unit tests of {@link GpxImageCorrelation} class.
 */
@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(MethodName.class)
@Timezone
class GpxImageCorrelationTest {
    GpxData gpx;
    GpxImageEntry ib, i0, i1, i2, i3, i4, i5, i6, i7;
    List<GpxImageEntry> images;
    IPreferences s;

    /**
     * Setup test.
     * @throws IOException if an error occurs during reading.
     * @throws SAXException if a SAX error occurs
     */
    @BeforeAll
    public void setUp() throws IOException, SAXException {
        s = Config.getPref();

        gpx = GpxReaderTest.parseGpxData(TestUtils.getTestDataRoot() + "tracks/tracks.gpx");
        assertEquals(5, gpx.tracks.size());
        assertEquals(1, gpx.tracks.iterator().next().getSegments().size());
        assertEquals(128, gpx.tracks.iterator().next().getSegments().iterator().next().getWayPoints().size());

        ib = new GpxImageEntry();
        ib.setExifTime(DateUtils.parseInstant("2016:01:03 11:54:58")); // 5 minutes before start of GPX

        i0 = new GpxImageEntry();
        i0.setExifTime(DateUtils.parseInstant("2016:01:03 11:59:54")); // 4 sec before start of GPX

        i1 = new GpxImageEntry();
        i1.setExifTime(DateUtils.parseInstant("2016:01:03 12:04:01"));
        i1.setPos(new CachedLatLon(2, 3)); //existing position inside the track, should always be overridden

        i2 = new GpxImageEntry();
        i2.setExifTime(DateUtils.parseInstant("2016:01:03 12:04:57"));

        i3 = new GpxImageEntry();
        i3.setExifTime(DateUtils.parseInstant("2016:01:03 12:05:05"));

        i4 = new GpxImageEntry(); //Image close to two points with elevation, but without time
        i4.setExifTime(DateUtils.parseInstant("2016:01:03 12:05:20"));

        i5 = new GpxImageEntry(); //between two tracks, closer to first
        i5.setExifTime(DateUtils.parseInstant("2016:01:03 12:07:00"));

        i6 = new GpxImageEntry(); //between two tracks, closer to second (more than 1 minute from any track)
        i6.setExifTime(DateUtils.parseInstant("2016:01:03 12:07:45"));

        i7 = new GpxImageEntry();
        i7.setExifTime(DateUtils.parseInstant("2021:01:01 00:00:00"));
        i7.setPos(new CachedLatLon(1, 2)); //existing position outside the track
        // the position should never be null (should keep the old position if not overridden, see #11710)

        images = Arrays.asList(ib, i0, i1, i2, i3, i4, i5, i6, i7);
    }

    @BeforeEach
    void clearTmp() {
        for (GpxImageEntry i : images) {
            i.discardTmp();
            i.createTmp();
        }
    }

    /**
     * Tests matching of images to a GPX track.
     * <p>
     * TEST #1: default settings
     * tag images within 2 minutes to tracks/segments, interpolate between segments only
     */
    @Test
    void testMatchGpxTrack1() {
        assertEquals(7, GpxImageCorrelation.matchGpxTrack(images, gpx, new GpxImageCorrelationSettings(0, false)));
        assertNull(ib.getPos());
        assertEquals(new CachedLatLon(47.19286847859621, 8.79732714034617), i0.getPos()); // start of track
        assertEquals(new CachedLatLon(47.196979885920882, 8.79541271366179), i1.getPos()); // exact match
        assertEquals(new CachedLatLon(47.197319911792874, 8.792139580473304), i3.getPos()); // exact match
        assertEquals(new CachedLatLon((47.197131179273129 + 47.197186248376966) / 2,
                (8.792974585667253 + 8.792809881269932) / 2), i2.getPos()); // interpolated
        assertEquals(new CachedLatLon(47.197568312311816, 8.790292849679897), i4.getPos()); // interpolated between points without timestamp
        // tagged to last WP of first track, because closer and within 2 min (default setting):
        assertEquals(new CachedLatLon(47.19819249585271, 8.78536943346262), i5.getPos());
        // tagged to first WP of second track, because closer and within 2 min (default setting):
        assertEquals(new CachedLatLon(47.20138901844621, 8.774476982653141), i6.getPos());
        assertEquals(new CachedLatLon(1, 2), i7.getPos()); //existing EXIF data is kept
        assertFalse(ib.hasNewGpsData() || i7.hasNewGpsData());
        assertTrue(i0.hasNewGpsData() && i1.hasNewGpsData() && i2.hasNewGpsData() && i3.hasNewGpsData()
                && i4.hasNewGpsData() && i5.hasNewGpsData() && i6.hasNewGpsData());
        // First waypoint has no speed in matchGpxTrack(). Speed is calculated
        // and not taken from GPX track.
        assertNull(ib.getSpeed());
        assertNull(i0.getSpeed());
        assertEquals(11.675317966018756, i1.getSpeed(), 0.000001);
        assertEquals(24.992418392716967, i2.getSpeed(), 0.000001);
        assertEquals(27.307968754679223, i3.getSpeed(), 0.000001);
        assertNull(ib.getElevation());
        assertNull(i0.getElevation());
        assertEquals(489.29, i1.getElevation(), 0.000001);
        assertEquals((490.40 + 489.75) / 2, i2.getElevation(), 0.000001);
        assertEquals(486.368333333, i3.getElevation(), 0.000001);
        // interpolated elevation between trackpoints with interpolated timestamps
        assertEquals(475.393978719, i4.getElevation(), 0.000001);
        assertNull(i5.getElevation());
        assertNull(i6.getElevation());

        assertNull(ib.getGpsInstant());
        assertEquals(DateUtils.parseInstant("2016:01:03 11:59:54"), i0.getGpsInstant()); // original time is kept
        assertEquals(DateUtils.parseInstant("2016:01:03 12:04:01"), i1.getGpsInstant());
        assertEquals(DateUtils.parseInstant("2016:01:03 12:04:57"), i2.getGpsInstant());
        assertEquals(DateUtils.parseInstant("2016:01:03 12:05:05"), i3.getGpsInstant());
    }

    /**
     * Tests matching of images to a GPX track.
     * <p>
     * TEST #2: Disable all interpolation and tagging close to tracks. Only i1-i4 are tagged
     */
    @Test
    void testMatchGpxTrack2() {
        s.putBoolean("geoimage.trk.tag", false);
        s.putBoolean("geoimage.trk.int", false);
        s.putBoolean("geoimage.seg.tag", false);
        s.putBoolean("geoimage.seg.int", false);

        assertEquals(4, GpxImageCorrelation.matchGpxTrack(images, gpx, new GpxImageCorrelationSettings(0, false)));
        assertNull(ib.getPos());
        assertNull(i0.getPos());
        assertEquals(new CachedLatLon(47.196979885920882, 8.79541271366179), i1.getPos());
        assertEquals(new CachedLatLon((47.197131179273129 + 47.197186248376966) / 2,
                (8.792974585667253 + 8.792809881269932) / 2), i2.getPos());
        assertEquals(new CachedLatLon(47.197319911792874, 8.792139580473304), i3.getPos());
        assertEquals(new CachedLatLon(47.197568312311816, 8.790292849679897), i4.getPos());
        assertNull(i5.getPos());
        assertNull(i6.getPos());
    }

    /**
     * Tests matching of images to a GPX track.
     * <p>
     * TEST #3: Disable all interpolation and allow tagging within 1 minute of a track. i0-i5 are tagged.
     * i6 will not be tagged, because it's 68 seconds away from the next waypoint in either direction
     * i7 will keep the old position
     */
    @Test
    void testMatchGpxTrack3() {
        s.putBoolean("geoimage.trk.tag", true);
        s.putBoolean("geoimage.trk.tag.time", true);
        s.putInt("geoimage.trk.tag.time.val", 1);
        s.putBoolean("geoimage.trk.int", false);
        s.putBoolean("geoimage.seg.tag", false);
        s.putBoolean("geoimage.seg.int", false);

        assertEquals(6, GpxImageCorrelation.matchGpxTrack(images, gpx, new GpxImageCorrelationSettings(0, false)));
        assertNull(ib.getPos());
        assertEquals(new CachedLatLon(47.19286847859621, 8.79732714034617), i0.getPos());
        assertEquals(new CachedLatLon(47.196979885920882, 8.79541271366179), i1.getPos());
        assertEquals(new CachedLatLon((47.197131179273129 + 47.197186248376966) / 2,
                (8.792974585667253 + 8.792809881269932) / 2), i2.getPos());
        assertEquals(new CachedLatLon(47.197319911792874, 8.792139580473304), i3.getPos());
        assertEquals(new CachedLatLon(47.197568312311816, 8.790292849679897), i4.getPos());
        assertEquals(new CachedLatLon(47.19819249585271, 8.78536943346262), i5.getPos());
        assertNull(i6.getPos());
        assertEquals(new CachedLatLon(1, 2), i7.getPos());
    }

    /**
     * Tests matching of images to a GPX track.
     * <p>
     * TEST #4: Force tagging (parameter forceTags=true, no change of configuration). All images will be tagged
     * i5-i6 will now be interpolated, therefore it will have an elevation and different coordinates than in tests above
     * i7 will be at the end of the track
     */
    @Test
    void testMatchGpxTrack4() {
        assertEquals(9, GpxImageCorrelation.matchGpxTrack(images, gpx, new GpxImageCorrelationSettings(0, true)));
        assertEquals(new CachedLatLon(47.19286847859621, 8.79732714034617), ib.getPos());
        assertEquals(new CachedLatLon(47.19286847859621, 8.79732714034617), i0.getPos());
        assertEquals(new CachedLatLon(47.196979885920882, 8.79541271366179), i1.getPos());
        assertEquals(new CachedLatLon((47.197131179273129 + 47.197186248376966) / 2,
                (8.792974585667253 + 8.792809881269932) / 2), i2.getPos());
        assertEquals(new CachedLatLon(47.197319911792874, 8.792139580473304), i3.getPos());
        assertEquals(new CachedLatLon(47.197568312311816, 8.790292849679897), i4.getPos());
        assertEquals(new CachedLatLon(47.198845306804905, 8.783144918860685), i5.getPos()); // interpolated between tracks
        assertEquals(new CachedLatLon(47.19985828931693, 8.77969308585768), i6.getPos()); // different values than in tests #1 and #3!

        assertEquals(447.894014085, i5.getElevation(), 0.000001);
        assertEquals(437.395070423, i6.getElevation(), 0.000001);

        assertEquals(new CachedLatLon(47.20126815140247, 8.77192972227931), i7.getPos());
    }

    /**
     * Tests matching of images to a GPX track.
     * <p>
     * TEST #5: Force tagging (parameter forceTags=false, but configuration changed).
     * Results same as #4
     */
    @Test
    void testMatchGpxTrack5() {
        s.putBoolean("geoimage.trk.tag", true);
        s.putBoolean("geoimage.trk.tag.time", false);
        s.putBoolean("geoimage.trk.int", true);
        s.putBoolean("geoimage.trk.int.time", false);
        s.putBoolean("geoimage.trk.int.dist", false);
        s.putBoolean("geoimage.seg.tag", false);
        s.putBoolean("geoimage.seg.int", false);
        s.putBoolean("geoimage.seg.tag.time", false);
        s.putBoolean("geoimage.seg.int", true);
        s.putBoolean("geoimage.seg.int.time", false);
        s.putBoolean("geoimage.seg.int.dist", false);

        assertEquals(9, GpxImageCorrelation.matchGpxTrack(images, gpx, new GpxImageCorrelationSettings(0, false)));
        assertEquals(new CachedLatLon(47.19286847859621, 8.79732714034617), ib.getPos());
        assertEquals(new CachedLatLon(47.19286847859621, 8.79732714034617), i0.getPos());
        assertEquals(new CachedLatLon(47.196979885920882, 8.79541271366179), i1.getPos());
        assertEquals(new CachedLatLon((47.197131179273129 + 47.197186248376966) / 2,
                (8.792974585667253 + 8.792809881269932) / 2), i2.getPos());
        assertEquals(new CachedLatLon(47.197319911792874, 8.792139580473304), i3.getPos());
        assertEquals(new CachedLatLon(47.197568312311816, 8.790292849679897), i4.getPos());
        assertEquals(new CachedLatLon(47.198845306804905, 8.783144918860685), i5.getPos());
        assertEquals(new CachedLatLon(47.19985828931693, 8.77969308585768), i6.getPos());

        assertEquals(447.894014085, i5.getElevation(), 0.000001);
        assertEquals(437.395070423, i6.getElevation(), 0.000001);

        assertEquals(new CachedLatLon(47.20126815140247, 8.77192972227931), i7.getPos());
    }

    /**
     * Tests matching of images to a GPX track.
     * <p>
     * TEST #6: Disable tagging but allow interpolation when tracks are less than 500m apart. i0-i4 are tagged.
     * i5-i6 will not be tagged, because the tracks are 897m apart.
     * not checking all the coordinates again, did that 5 times already, just the number of matched images
     */
    @Test
    void testMatchGpxTrack6() {
        s.putBoolean("geoimage.trk.tag", false);
        s.putBoolean("geoimage.trk.int", true);
        s.putBoolean("geoimage.trk.int.time", false);
        s.putBoolean("geoimage.trk.int.dist", true);
        s.putInt("geoimage.trk.int.dist.val", 500);
        s.putBoolean("geoimage.seg.tag", false);
        s.putBoolean("geoimage.seg.int", false);

        assertEquals(4, GpxImageCorrelation.matchGpxTrack(images, gpx, new GpxImageCorrelationSettings(0, false)));
    }

    /**
     * Tests matching of images to a GPX track.
     * <p>
     * TEST #7: Disable tagging but allow interpolation when tracks are less than 1000m apart. i0-i6 are tagged.
     * i5-i6 will be tagged, because the tracks are 897m apart.
     */
    @Test
    void testMatchGpxTrack7() {
        s.putBoolean("geoimage.trk.tag", false);
        s.putBoolean("geoimage.trk.int", true);
        s.putBoolean("geoimage.trk.int.time", false);
        s.putBoolean("geoimage.trk.int.dist", true);
        s.putInt("geoimage.trk.int.dist.val", 1000);
        s.putBoolean("geoimage.seg.tag", false);
        s.putBoolean("geoimage.seg.int", false);

        assertEquals(6, GpxImageCorrelation.matchGpxTrack(images, gpx, new GpxImageCorrelationSettings(0, false)));
    }

    /**
     * Tests matching of images to a GPX track.
     * <p>
     * TEST #8: Disable tagging but allow interpolation when tracks are less than 2 min apart. i0-i4 are tagged.
     * i5-i6 will not be tagged, because the tracks are 2.5min apart.
     */
    @Test
    void testMatchGpxTrack8() {
        s.putBoolean("geoimage.trk.tag", false);
        s.putBoolean("geoimage.trk.int", true);
        s.putBoolean("geoimage.trk.int.time", true);
        s.putInt("geoimage.trk.int.time.val", 2);
        s.putBoolean("geoimage.trk.int.dist", false);
        s.putBoolean("geoimage.seg.tag", false);
        s.putBoolean("geoimage.seg.int", false);

        assertEquals(4, GpxImageCorrelation.matchGpxTrack(images, gpx, new GpxImageCorrelationSettings(0, false)));
    }

    /**
     * Tests matching of images to a GPX track.
     * <p>
     * TEST #9: Disable tagging but allow interpolation when tracks are less than 3 min apart. i0-i6 are tagged.
     * i5-i6 will be tagged, because the tracks are 2.5min apart.
     */
    @Test
    void testMatchGpxTrack9() {
        s.putBoolean("geoimage.trk.tag", false);
        s.putBoolean("geoimage.trk.int", true);
        s.putBoolean("geoimage.trk.int.time", true);
        s.putInt("geoimage.trk.int.time.val", 3);
        s.putBoolean("geoimage.trk.int.dist", false);
        s.putBoolean("geoimage.seg.tag", false);
        s.putBoolean("geoimage.seg.int", false);

        assertEquals(6, GpxImageCorrelation.matchGpxTrack(images, gpx, new GpxImageCorrelationSettings(0, false)));
    }

    /**
     * Unit test of {@link GpxImageCorrelation#getElevation}
     */
    @Test
    void testGetElevation() {
        assertNull(GpxImageCorrelation.getElevation(null));
        WayPoint wp = new WayPoint(LatLon.ZERO);
        assertNull(GpxImageCorrelation.getElevation(wp));
        wp.put(GpxConstants.PT_ELE, "");
        assertNull(GpxImageCorrelation.getElevation(wp));
        wp.put(GpxConstants.PT_ELE, "not a number");
        assertNull(GpxImageCorrelation.getElevation(wp));
        wp.put(GpxConstants.PT_ELE, "150.0");
        assertEquals(Double.valueOf(150.0d), GpxImageCorrelation.getElevation(wp));
    }

    /**
     * Unit test of {@link GpxImageCorrelation#getHPosErr}
     */
    @Test
    void testGetHorizontalPosError() {
        assertNull(GpxImageCorrelation.getHPosErr(null));
        WayPoint wp = new WayPoint(LatLon.ZERO);
        assertNull(GpxImageCorrelation.getHPosErr(wp));
        wp.put(GpxConstants.PT_STD_HDEV, 1.5f);
        assertEquals(1.5f, GpxImageCorrelation.getHPosErr(wp));
    }

    /**
     * Unit test of {@link GpxImageCorrelation#getGpsDop}
     */
    @Test
    void testGetGpsDop() {
        assertNull(GpxImageCorrelation.getGpsDop(null));
        WayPoint wp = new WayPoint(LatLon.ZERO);
        assertNull(GpxImageCorrelation.getGpsDop(wp));
        wp.put(GpxConstants.PT_HDOP, 2.1f);
        assertEquals(2.1f, GpxImageCorrelation.getGpsDop(wp));
        wp.put(GpxConstants.PT_PDOP, 0.9f);
        assertEquals(0.9f, GpxImageCorrelation.getGpsDop(wp));
        wp.put(GpxConstants.PT_PDOP, 1.2d);
        assertEquals(1.2d, GpxImageCorrelation.getGpsDop(wp));
    }

    /**
     * Unit test of {@link GpxImageCorrelation#getGpsTrack}
     */
    @Test
    void testGetGpsTrack() {
        assertNull(GpxImageCorrelation.getHPosErr(null));
        WayPoint wp = new WayPoint(LatLon.ZERO);
        assertNull(GpxImageCorrelation.getGpsTrack(wp));
        wp.put(GpxConstants.PT_COURSE, "");
        assertNull(GpxImageCorrelation.getGpsTrack(wp));
        wp.put(GpxConstants.PT_COURSE, "not a number");
        assertNull(GpxImageCorrelation.getGpsTrack(wp));
        wp.put(GpxConstants.PT_COURSE, "167.0");
        assertEquals(Double.valueOf(167.0d), GpxImageCorrelation.getGpsTrack(wp), 0.01d);
    }

    /**
     * Unit test of {@link GpxImageCorrelation#getGpsProcMethod}
     */
    @Test
    void testGetGpsProcMethod() {
        final List<String> positionningModes = Arrays.asList("none", "manual", "estimated", "2d", "3d", "dgps", "float rtk", "rtk");
        assertNull(GpxImageCorrelation.getGpsProcMethod(null, null, positionningModes));
        assertNull(GpxImageCorrelation.getGpsProcMethod("", "", positionningModes));
        assertNull(GpxImageCorrelation.getGpsProcMethod("3d", null, positionningModes));
        assertNull(GpxImageCorrelation.getGpsProcMethod("", "dgps", positionningModes));
        assertEquals("MANUAL CORRELATION", GpxImageCorrelation.getGpsProcMethod("manual", "rtk", positionningModes));
        assertEquals("ESTIMATED CORRELATION", GpxImageCorrelation.getGpsProcMethod("estimated", "2d", positionningModes));
        assertEquals("GNSS DGPS CORRELATION", GpxImageCorrelation.getGpsProcMethod("rtk", "dgps", positionningModes));
        assertEquals("GNSS RTK_FLOAT CORRELATION", GpxImageCorrelation.getGpsProcMethod("float rtk", "rtk", positionningModes));
        assertEquals("GNSS RTK_FIX CORRELATION", GpxImageCorrelation.getGpsProcMethod("rtk", "rtk", positionningModes));
    }

    /**
     * Unit test of {@link GpxImageCorrelation#getGps2d3dMode}
     */
    @Test
    void testGetGps2d3dMode() {
        final List<String> positionningModes = Arrays.asList("none", "manual", "estimated", "2d", "3d", "dgps", "float rtk", "rtk");
        assertNull(GpxImageCorrelation.getGps2d3dMode(null, null, positionningModes));
        assertNull(GpxImageCorrelation.getGps2d3dMode("", "", positionningModes));
        assertNull(GpxImageCorrelation.getGps2d3dMode("3d", null, positionningModes));
        assertNull(GpxImageCorrelation.getGps2d3dMode("", "dgps", positionningModes));
        assertNull(GpxImageCorrelation.getGps2d3dMode("estimated", "rtk", positionningModes));
        assertEquals(2, GpxImageCorrelation.getGps2d3dMode("2d", "2d", positionningModes));
        assertEquals(2, GpxImageCorrelation.getGps2d3dMode("2d", "3d", positionningModes));
        assertEquals(3, GpxImageCorrelation.getGps2d3dMode("3d", "dgps", positionningModes));
    }
}
