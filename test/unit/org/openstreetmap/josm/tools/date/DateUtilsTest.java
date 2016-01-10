// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.date;

import static org.junit.Assert.assertEquals;

import java.util.TimeZone;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.tools.UncheckedParseException;

/**
 * Unit tests of {@link DateUtils} class.
 */
public class DateUtilsTest {

    /**
     * Allows to override the timezone used in {@link DateUtils} for unit tests.
     * @param zone the timezone to use
     */
    public static void setTimeZone(TimeZone zone) {
        DateUtils.setTimeZone(zone);
    }

    /**
     * Test to parse date as returned for map data.
     */
    @Test
    public void testMapDate() {
        assertEquals(1344870637000L, DateUtils.fromString("2012-08-13T15:10:37Z").getTime());
    }

    /**
     * Test to parse date as returned for note data.
     */
    @Test
    public void testNoteDate() {
        assertEquals(1417298930000L, DateUtils.fromString("2014-11-29 22:08:50 UTC").getTime());
    }

    /**
     * Test to parse date as used in EXIF structures.
     */
    @Test
    public void testExifDate() {
        setTimeZone(TimeZone.getTimeZone("GMT+8:00")); // parsing is timezone aware
        assertEquals(1443038712000L - 8 * 3600 * 1000, DateUtils.fromString("2015:09:23 20:05:12").getTime());
        assertEquals(1443038712888L - 8 * 3600 * 1000, DateUtils.fromString("2015:09:23 20:05:12.888").getTime());
    }

    /**
     * Test to parse date as used in GPX files
     */
    @Test
    public void testGPXDate() {
        assertEquals(1277465405000L, DateUtils.fromString("2010-06-25T11:30:05.000Z").getTime());
    }

    /**
     * Test to parse date as defined in <a href="https://tools.ietf.org/html/rfc3339">RFC 3339</a>
     */
    @Test
    public void testRfc3339() {
        // examples taken from RFC
        assertEquals(482196050520L, DateUtils.fromString("1985-04-12T23:20:50.52Z").getTime());
        assertEquals(851042397000L, DateUtils.fromString("1996-12-19T16:39:57-08:00").getTime());
        assertEquals(-1041337172130L, DateUtils.fromString("1937-01-01T12:00:27.87+00:20").getTime());
    }

    /**
     * Verifies that parsing an illegal date throws a {@link UncheckedParseException}
     */
    @Test(expected = UncheckedParseException.class)
    public void testIllegalDate() {
        DateUtils.fromString("2014-");
    }
}
