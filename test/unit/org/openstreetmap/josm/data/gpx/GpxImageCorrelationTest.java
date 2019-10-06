// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.CachedLatLon;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.io.GpxReaderTest;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.spi.preferences.IPreferences;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.date.DateUtils;
import org.openstreetmap.josm.tools.date.DateUtilsTest;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link GpxImageCorrelation} class.
 */
public class GpxImageCorrelationTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        DateUtilsTest.setTimeZone(DateUtils.UTC);
    }

    /**
     * Tests matching of images to a GPX track.
     * @throws Exception if the track cannot be parsed
     */
    @Test
    public void testMatchGpxTrack() throws Exception {
        IPreferences s = Config.getPref();
        final GpxData gpx = GpxReaderTest.parseGpxData(TestUtils.getTestDataRoot() + "tracks/tracks.gpx");
        assertEquals(5, gpx.tracks.size());
        assertEquals(1, gpx.tracks.iterator().next().getSegments().size());
        assertEquals(128, gpx.tracks.iterator().next().getSegments().iterator().next().getWayPoints().size());

        final GpxImageEntry ib = new GpxImageEntry();
        ib.setExifTime(DateUtils.fromString("2016:01:03 11:54:58")); // 5 minutes before start of GPX
        ib.createTmp();
        final GpxImageEntry i0 = new GpxImageEntry();
        i0.setExifTime(DateUtils.fromString("2016:01:03 11:59:54")); // 4 sec before start of GPX
        i0.createTmp();
        final GpxImageEntry i1 = new GpxImageEntry();
        i1.setExifTime(DateUtils.fromString("2016:01:03 12:04:01"));
        i1.createTmp();
        final GpxImageEntry i2 = new GpxImageEntry();
        i2.setExifTime(DateUtils.fromString("2016:01:03 12:04:57"));
        i2.createTmp();
        final GpxImageEntry i3 = new GpxImageEntry();
        i3.setExifTime(DateUtils.fromString("2016:01:03 12:05:05"));
        i3.createTmp();
        final GpxImageEntry i4 = new GpxImageEntry(); //Image close to two points with elevation, but without time
        i4.setExifTime(DateUtils.fromString("2016:01:03 12:05:20"));
        i4.createTmp();
        final GpxImageEntry i5 = new GpxImageEntry(); //between two tracks, closer to first
        i5.setExifTime(DateUtils.fromString("2016:01:03 12:07:00"));
        i5.createTmp();
        final GpxImageEntry i6 = new GpxImageEntry(); //between two tracks, closer to second (more than 1 minute from any track)
        i6.setExifTime(DateUtils.fromString("2016:01:03 12:07:45"));
        i6.createTmp();

        List<GpxImageEntry> images = Arrays.asList(ib, i0, i1, i2, i3, i4, i5, i6);

        // TEST #1: default settings
        // tag images within 2 minutes to tracks/segments, interpolate between segments only
        assertEquals(7, GpxImageCorrelation.matchGpxTrack(images, gpx, 0, false));
        assertEquals(null, ib.getPos());
        assertEquals(new CachedLatLon(47.19286847859621, 8.79732714034617), i0.getPos()); // start of track
        assertEquals(new CachedLatLon(47.196979885920882, 8.79541271366179), i1.getPos()); // exact match
        assertEquals(new CachedLatLon(47.197319911792874, 8.792139580473304), i3.getPos()); // exact match
        assertEquals(new CachedLatLon((47.197131179273129 + 47.197186248376966) / 2, (8.792974585667253 + 8.792809881269932) / 2),
                i2.getPos()); // interpolated
        assertEquals(new CachedLatLon(47.197568312311816, 8.790292849679897),
                i4.getPos()); // interpolated between points without timestamp
        assertEquals(new CachedLatLon(47.19819249585271, 8.78536943346262),
                i5.getPos()); // tagged to last WP of first track, because closer and within 2 min (default setting)
        assertEquals(new CachedLatLon(47.20138901844621, 8.774476982653141),
                i6.getPos()); // tagged to first WP of second track, because closer and within 2 min (default setting)
        assertFalse(ib.hasNewGpsData());
        assertTrue(i0.hasNewGpsData() && i1.hasNewGpsData() && i2.hasNewGpsData() && i3.hasNewGpsData()
                && i4.hasNewGpsData() && i5.hasNewGpsData() && i6.hasNewGpsData());
        // First waypoint has no speed in matchGpxTrack(). Speed is calculated
        // and not taken from GPX track.
        assertEquals(null, ib.getSpeed());
        assertEquals(null, i0.getSpeed());
        assertEquals(Double.valueOf(11.675317966018756), i1.getSpeed(), 0.000001);
        assertEquals(Double.valueOf(24.992418392716967), i2.getSpeed(), 0.000001);
        assertEquals(Double.valueOf(27.307968754679223), i3.getSpeed(), 0.000001);
        assertEquals(null, ib.getElevation());
        assertEquals(null, i0.getElevation());
        assertEquals(Double.valueOf(489.29), i1.getElevation(), 0.000001);
        assertEquals(Double.valueOf((490.40 + 489.75) / 2), i2.getElevation(), 0.000001);
        assertEquals(Double.valueOf(486.368333333), i3.getElevation(), 0.000001);
        // interpolated elevation between trackpoints with interpolated timestamps
        assertEquals(Double.valueOf(475.393978719), i4.getElevation(), 0.000001);
        assertEquals(null, i5.getElevation());
        assertEquals(null, i6.getElevation());

        assertEquals(null, ib.getGpsTime());
        assertEquals(DateUtils.fromString("2016:01:03 11:59:54"), i0.getGpsTime()); // original time is kept
        assertEquals(DateUtils.fromString("2016:01:03 12:04:01"), i1.getGpsTime());
        assertEquals(DateUtils.fromString("2016:01:03 12:04:57"), i2.getGpsTime());
        assertEquals(DateUtils.fromString("2016:01:03 12:05:05"), i3.getGpsTime());

        clearTmp(images);

        // TEST #2: Disable all interpolation and tagging close to tracks. Only i1-i4 are tagged

        s.putBoolean("geoimage.trk.tag", false);
        s.putBoolean("geoimage.trk.int", false);
        s.putBoolean("geoimage.seg.tag", false);
        s.putBoolean("geoimage.seg.int", false);

        assertEquals(4, GpxImageCorrelation.matchGpxTrack(images, gpx, 0, false));
        assertEquals(null, ib.getPos());
        assertEquals(null, i0.getPos());
        assertEquals(new CachedLatLon(47.196979885920882, 8.79541271366179), i1.getPos());
        assertEquals(new CachedLatLon((47.197131179273129 + 47.197186248376966) / 2, (8.792974585667253 + 8.792809881269932) / 2), i2.getPos());
        assertEquals(new CachedLatLon(47.197319911792874, 8.792139580473304), i3.getPos());
        assertEquals(new CachedLatLon(47.197568312311816, 8.790292849679897), i4.getPos());
        assertEquals(null, i5.getPos());
        assertEquals(null, i6.getPos());

        clearTmp(images);

        // TEST #3: Disable all interpolation and allow tagging within 1 minute of a track. i0-i5 are tagged.
        // i6 will not be tagged, because it's 68 seconds away from the next waypoint in either direction

        s.putBoolean("geoimage.trk.tag", true);
        s.putBoolean("geoimage.trk.tag.time", true);
        s.putInt("geoimage.trk.tag.time.val", 1);

        s.putBoolean("geoimage.trk.int", false);
        s.putBoolean("geoimage.seg.tag", false);
        s.putBoolean("geoimage.seg.int", false);

        assertEquals(6, GpxImageCorrelation.matchGpxTrack(images, gpx, 0, false));
        assertEquals(null, ib.getPos());
        assertEquals(new CachedLatLon(47.19286847859621, 8.79732714034617), i0.getPos());
        assertEquals(new CachedLatLon(47.196979885920882, 8.79541271366179), i1.getPos());
        assertEquals(new CachedLatLon((47.197131179273129 + 47.197186248376966) / 2, (8.792974585667253 + 8.792809881269932) / 2), i2.getPos());
        assertEquals(new CachedLatLon(47.197319911792874, 8.792139580473304), i3.getPos());
        assertEquals(new CachedLatLon(47.197568312311816, 8.790292849679897), i4.getPos());
        assertEquals(new CachedLatLon(47.19819249585271, 8.78536943346262), i5.getPos());
        assertEquals(null, i6.getPos());

        clearTmp(images);

        // TEST #4: Force tagging (parameter forceTags=true, no change of configuration). All images will be tagged
        // i5-i6 will now be interpolated, therefore it will have an elevation and different coordinates than in tests above

        assertEquals(8, GpxImageCorrelation.matchGpxTrack(images, gpx, 0, true));
        assertEquals(new CachedLatLon(47.19286847859621, 8.79732714034617), ib.getPos());
        assertEquals(new CachedLatLon(47.19286847859621, 8.79732714034617), i0.getPos());
        assertEquals(new CachedLatLon(47.196979885920882, 8.79541271366179), i1.getPos());
        assertEquals(new CachedLatLon((47.197131179273129 + 47.197186248376966) / 2, (8.792974585667253 + 8.792809881269932) / 2), i2.getPos());
        assertEquals(new CachedLatLon(47.197319911792874, 8.792139580473304), i3.getPos());
        assertEquals(new CachedLatLon(47.197568312311816, 8.790292849679897), i4.getPos());
        assertEquals(new CachedLatLon(47.198845306804905, 8.783144918860685), i5.getPos()); // interpolated between tracks
        assertEquals(new CachedLatLon(47.19985828931693, 8.77969308585768), i6.getPos()); // different values than in tests #1 and #3!

        assertEquals(Double.valueOf(447.894014085), i5.getElevation(), 0.000001);
        assertEquals(Double.valueOf(437.395070423), i6.getElevation(), 0.000001);

        clearTmp(images);

        // TEST #5: Force tagging (parameter forceTags=false, but configuration changed).
        // Results same as #4

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

        assertEquals(8, GpxImageCorrelation.matchGpxTrack(images, gpx, 0, false));
        assertEquals(new CachedLatLon(47.19286847859621, 8.79732714034617), ib.getPos());
        assertEquals(new CachedLatLon(47.19286847859621, 8.79732714034617), i0.getPos());
        assertEquals(new CachedLatLon(47.196979885920882, 8.79541271366179), i1.getPos());
        assertEquals(new CachedLatLon((47.197131179273129 + 47.197186248376966) / 2, (8.792974585667253 + 8.792809881269932) / 2), i2.getPos());
        assertEquals(new CachedLatLon(47.197319911792874, 8.792139580473304), i3.getPos());
        assertEquals(new CachedLatLon(47.197568312311816, 8.790292849679897), i4.getPos());
        assertEquals(new CachedLatLon(47.198845306804905, 8.783144918860685), i5.getPos());
        assertEquals(new CachedLatLon(47.19985828931693, 8.77969308585768), i6.getPos());

        assertEquals(Double.valueOf(447.894014085), i5.getElevation(), 0.000001);
        assertEquals(Double.valueOf(437.395070423), i6.getElevation(), 0.000001);

        clearTmp(images);

        // TEST #6: Disable tagging but allow interpolation when tracks are less than 500m apart. i0-i4 are tagged.
        // i5-i6 will not be tagged, because the tracks are 897m apart.
        // not checking all the coordinates again, did that 5 times already, just the number of matched images

        s.putBoolean("geoimage.trk.tag", false);
        s.putBoolean("geoimage.trk.int", true);
        s.putBoolean("geoimage.trk.int.time", false);
        s.putBoolean("geoimage.trk.int.dist", true);
        s.putInt("geoimage.trk.int.dist.val", 500);
        s.putBoolean("geoimage.seg.tag", false);
        s.putBoolean("geoimage.seg.int", false);

        assertEquals(4, GpxImageCorrelation.matchGpxTrack(images, gpx, 0, false));
        clearTmp(images);

        // TEST #7: Disable tagging but allow interpolation when tracks are less than 1000m apart. i0-i6 are tagged.
        // i5-i6 will be tagged, because the tracks are 897m apart.

        s.putInt("geoimage.trk.int.dist.val", 1000);

        assertEquals(6, GpxImageCorrelation.matchGpxTrack(images, gpx, 0, false));
        clearTmp(images);

        // TEST #8: Disable tagging but allow interpolation when tracks are less than 2 min apart. i0-i4 are tagged.
        // i5-i6 will not be tagged, because the tracks are 2.5min apart.

        s.putBoolean("geoimage.trk.tag", false);
        s.putBoolean("geoimage.trk.int", true);
        s.putBoolean("geoimage.trk.int.time", true);
        s.putInt("geoimage.trk.int.time.val", 2);
        s.putBoolean("geoimage.trk.int.dist", false);
        s.putBoolean("geoimage.seg.tag", false);
        s.putBoolean("geoimage.seg.int", false);

        assertEquals(4, GpxImageCorrelation.matchGpxTrack(images, gpx, 0, false));
        clearTmp(images);

        // TEST #9: Disable tagging but allow interpolation when tracks are less than 3 min apart. i0-i6 are tagged.
        // i5-i6 will be tagged, because the tracks are 2.5min apart.

        s.putInt("geoimage.trk.int.time.val", 3);

        assertEquals(6, GpxImageCorrelation.matchGpxTrack(images, gpx, 0, false));

    }

    private void clearTmp(List<GpxImageEntry> imgs) {
        for (GpxImageEntry i : imgs) {
            i.discardTmp();
            i.createTmp();
        }
    }

    /**
     * Unit test of {@link GpxImageCorrelation#getElevation}
     */
    @Test
    public void testGetElevation() {
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
}
