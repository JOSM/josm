// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import static org.junit.Assert.assertEquals;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.TimeZone;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.io.GpxReaderTest;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.date.DateUtils;
import org.openstreetmap.josm.tools.date.DateUtilsTest;

/**
 * Unit tests of {@link CorrelateGpxWithImagesTest} class.
 */
public class CorrelateGpxWithImagesTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        DateUtilsTest.setTimeZone(TimeZone.getTimeZone("UTC"));
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

        assertEquals(4, CorrelateGpxWithImages.matchGpxTrack(Arrays.asList(i0, i1, i2, i3), gpx, 0));
        assertEquals(new LatLon(47.19286847859621, 8.79732714034617), i0.getPos()); // start of track
        assertEquals(new LatLon(47.196979885920882, 8.79541271366179), i1.getPos()); // exact match
        assertEquals(new LatLon(47.197319911792874, 8.792139580473304), i3.getPos()); // exact match
        assertEquals(new LatLon((47.197131179273129 + 47.197186248376966) / 2, (8.792974585667253 + 8.792809881269932) / 2),
                i2.getPos()); // interpolated
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
        assertEquals(Pair.create(0.0, -4L), CorrelateGpxWithImages.autoGuess(Collections.singletonList(i0), gpx));
    }

    @Test
    public void testFormatTimezone() throws Exception {
        assertEquals("+1:00", CorrelateGpxWithImages.formatTimezone(1));
        assertEquals("+6:30", CorrelateGpxWithImages.formatTimezone(6.5));
        assertEquals("-6:30", CorrelateGpxWithImages.formatTimezone(-6.5));
        assertEquals("+3:08", CorrelateGpxWithImages.formatTimezone(Math.PI));
        assertEquals("+2:43", CorrelateGpxWithImages.formatTimezone(Math.E));
    }

    @Test
    public void testParseTimezone() throws ParseException {
        assertEquals(1, CorrelateGpxWithImages.parseTimezone("+01:00"), 1e-3);
        assertEquals(1, CorrelateGpxWithImages.parseTimezone("+1:00"), 1e-3);
        assertEquals(1.5, CorrelateGpxWithImages.parseTimezone("+01:30"), 1e-3);
        assertEquals(11.5, CorrelateGpxWithImages.parseTimezone("+11:30"), 1e-3);
    }

    @Test
    public void testParseOffest() throws ParseException {
        assertEquals(0, CorrelateGpxWithImages.parseOffset("0"));
        assertEquals(4242L, CorrelateGpxWithImages.parseOffset("4242"));
        assertEquals(-4242L, CorrelateGpxWithImages.parseOffset("-4242"));
        assertEquals(0L, CorrelateGpxWithImages.parseOffset("-0"));
    }
}
