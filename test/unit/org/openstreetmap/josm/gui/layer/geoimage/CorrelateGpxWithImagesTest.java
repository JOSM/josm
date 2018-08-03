// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.coor.CachedLatLon;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.io.GpxReaderTest;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.date.DateUtils;
import org.openstreetmap.josm.tools.date.DateUtilsTest;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link CorrelateGpxWithImages} class.
 */
public class CorrelateGpxWithImagesTest {

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
        final GpxData gpx = GpxReaderTest.parseGpxData("data_nodist/2094047.gpx");
        assertEquals(4, gpx.tracks.size());
        assertEquals(1, gpx.tracks.iterator().next().getSegments().size());
        assertEquals(185, gpx.tracks.iterator().next().getSegments().iterator().next().getWayPoints().size());

        final ImageEntry ib = new ImageEntry();
        ib.setExifTime(DateUtils.fromString("2016:01:03 11:54:58")); // 5 minutes before start of GPX
        ib.createTmp();
        final ImageEntry i0 = new ImageEntry();
        i0.setExifTime(DateUtils.fromString("2016:01:03 11:59:54")); // 4 sec before start of GPX
        i0.createTmp();
        final ImageEntry i1 = new ImageEntry();
        i1.setExifTime(DateUtils.fromString("2016:01:03 12:04:01"));
        i1.createTmp();
        final ImageEntry i2 = new ImageEntry();
        i2.setExifTime(DateUtils.fromString("2016:01:03 12:04:57"));
        i2.createTmp();
        final ImageEntry i3 = new ImageEntry();
        i3.setExifTime(DateUtils.fromString("2016:01:03 12:05:05"));
        i3.createTmp();

        assertEquals(4, CorrelateGpxWithImages.matchGpxTrack(Arrays.asList(ib, i0, i1, i2, i3), gpx, 0));
        assertEquals(new CachedLatLon(47.19286847859621, 8.79732714034617), i0.getPos()); // start of track
        assertEquals(new CachedLatLon(47.196979885920882, 8.79541271366179), i1.getPos()); // exact match
        assertEquals(new CachedLatLon(47.197319911792874, 8.792139580473304), i3.getPos()); // exact match
        assertEquals(new CachedLatLon((47.197131179273129 + 47.197186248376966) / 2, (8.792974585667253 + 8.792809881269932) / 2),
                i2.getPos()); // interpolated
        assertFalse(ib.hasNewGpsData());
        assertTrue(i0.hasNewGpsData());
        assertTrue(i1.hasNewGpsData());
        assertTrue(i2.hasNewGpsData());
        assertTrue(i3.hasNewGpsData());
        // First waypoint has no speed in matchGpxTrack(). Speed is calculated
        // and not taken from GPX track.
        assertEquals(null, ib.getSpeed());
        assertEquals(null, i0.getSpeed());
        assertEquals(Double.valueOf(11.675317966018756), i1.getSpeed(), 0.000001);
        assertEquals(Double.valueOf(24.992418392716967), i2.getSpeed(), 0.000001);
        assertEquals(Double.valueOf(27.307968754679223), i3.getSpeed(), 0.000001);
        assertEquals(null, ib.getElevation());
        assertEquals(Double.valueOf(471.86), i0.getElevation(), 0.000001);
        assertEquals(Double.valueOf(489.29), i1.getElevation(), 0.000001);
        assertEquals(Double.valueOf((490.40 + 489.75) / 2), i2.getElevation(), 0.000001);
        assertEquals(Double.valueOf(486.368333333), i3.getElevation(), 0.000001);
        assertEquals(null, ib.getGpsTime());
        assertEquals(DateUtils.fromString("2016:01:03 11:59:54"), i0.getGpsTime()); // original time is kept
        assertEquals(DateUtils.fromString("2016:01:03 12:04:01"), i1.getGpsTime());
        assertEquals(DateUtils.fromString("2016:01:03 12:04:57"), i2.getGpsTime());
        assertEquals(DateUtils.fromString("2016:01:03 12:05:05"), i3.getGpsTime());
    }

    /**
     * Tests automatic guessing of timezone/offset
     * @throws Exception if an error occurs
     */
    @Test
    public void testAutoGuess() throws Exception {
        final GpxData gpx = GpxReaderTest.parseGpxData("data_nodist/2094047.gpx");
        final ImageEntry i0 = new ImageEntry();
        i0.setExifTime(DateUtils.fromString("2016:01:03 11:59:54")); // 4 sec before start of GPX
        i0.createTmp();
        assertEquals(Pair.create(Timezone.ZERO, Offset.seconds(-4)),
                CorrelateGpxWithImages.autoGuess(Collections.singletonList(i0), gpx));
    }
}
