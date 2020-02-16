// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.UncheckedParseException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.trajano.commons.testing.UtilityClassTestUtil;

/**
 * Unit tests of {@link DateUtils} class.
 */
public class DateUtilsTest {

    /**
     * Set the timezone and timeout.
     * <p>
     * Timeouts need to be disabled because we change the time zone.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().i18n().preferences();

    /**
     * Tests that {@code DateUtils} satisfies utility class criterias.
     * @throws ReflectiveOperationException if an error occurs
     */
    @Test
    public void testUtilityClass() throws ReflectiveOperationException {
        UtilityClassTestUtil.assertUtilityClassWellDefined(DateUtils.class);
    }

    /**
     * Allows to override the timezone used in {@link DateUtils} for unit tests.
     * @param zone the timezone to use
     */
    public static void setTimeZone(TimeZone zone) {
        TimeZone.setDefault(zone);
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
        assertEquals(1443038712000L, DateUtils.fromString("2015:09:23 20:05:12").getTime());
        assertEquals(1443038712888L, DateUtils.fromString("2015:09:23 20:05:12.888").getTime());
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

    /**
     * Tests that formatting a date w/ milliseconds does not cause incorrect parsing afterwards
     */
    @Test
    public void testFormattingMillisecondsDoesNotCauseIncorrectParsing() {
        DateUtils.fromDate(new Date(123));
        assertEquals(1453694709000L, DateUtils.fromString("2016-01-25T04:05:09.000Z").getTime());
        assertEquals(1453694709200L, DateUtils.fromString("2016-01-25T04:05:09.200Z").getTime());
        assertEquals(1453694709400L, DateUtils.fromString("2016-01-25T04:05:09.400Z").getTime());
    }

    /**
     * Unit test of {@link DateUtils#fromTimestamp} method.
     */
    @Test
    public void testFromTimestamp() {
        assertEquals("1970-01-01T00:00:00Z", DateUtils.fromTimestamp(0));
        assertEquals("2001-09-09T01:46:40Z", DateUtils.fromTimestamp(1000000000));
        assertEquals("2038-01-19T03:14:07Z", DateUtils.fromTimestamp(Integer.MAX_VALUE));
    }

    /**
     * Unit test of {@link DateUtils#fromDate} method.
     */
    @Test
    public void testFromDate() {
        assertEquals("1970-01-01T00:00:00Z", DateUtils.fromDate(new Date(0)));
        assertEquals("1970-01-01T00:00:00.1Z", DateUtils.fromDate(new Date(100)));
        assertEquals("1970-01-01T00:00:00.12Z", DateUtils.fromDate(new Date(120)));
        assertEquals("1970-01-01T00:00:00.123Z", DateUtils.fromDate(new Date(123)));
        assertEquals("2016-01-25T04:05:09Z", DateUtils.fromDate(new Date(1453694709000L)));
    }

    /**
     * Unit test of {@link DateUtils#formatTime} method.
     */
    @Test
    public void testFormatTime() {
        assertEquals("12:00 AM", DateUtils.formatTime(new Date(0), DateFormat.SHORT));
        assertEquals("1:00 AM", DateUtils.formatTime(new Date(60 * 60 * 1000), DateFormat.SHORT));
        assertEquals("12:00 AM", DateUtils.formatTime(new Date(999), DateFormat.SHORT));
        // ignore seconds
        assertEquals("12:00 AM", DateUtils.formatTime(new Date(5999), DateFormat.SHORT));

        setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));
        assertEquals("1:00:00 AM CET", DateUtils.formatTime(new Date(0), DateFormat.LONG));
    }

    /**
     * Unit test of {@link DateUtils#formatDate} method.
     */
    @Test
    public void testFormatDate() {
        assertEquals("1/1/70", DateUtils.formatDate(new Date(123), DateFormat.SHORT));
        assertEquals("January 1, 1970", DateUtils.formatDate(new Date(123), DateFormat.LONG));
    }

    /**
     * Unit test of {@link DateUtils#tsFromString} method.
     */
    @Test
    public void testTsFromString() {
        // UTC times
        assertEquals(1459641600000L, DateUtils.tsFromString("2016-04-03"));
        assertEquals(1459695600000L, DateUtils.tsFromString("2016-04-03T15:00:00Z"));
        assertEquals(1459695600000L, DateUtils.tsFromString("2016-04-03T15:00:00"));
        assertEquals(1459695600000L, DateUtils.tsFromString("2016-04-03 15:00:00 UTC"));
        assertEquals(1459695600000L, DateUtils.tsFromString("2016-04-03T15:00:00+00"));
        assertEquals(1459695600000L, DateUtils.tsFromString("2016-04-03T15:00:00-00"));
        assertEquals(1459695600000L, DateUtils.tsFromString("2016-04-03T15:00:00+00:00"));
        assertEquals(1459695600000L, DateUtils.tsFromString("2016-04-03T15:00:00-00:00"));

        // UTC times with millis
        assertEquals(1459695600000L, DateUtils.tsFromString("2016-04-03T15:00:00.000Z"));
        assertEquals(1459695600000L, DateUtils.tsFromString("2016-04-03T15:00:00.000"));
        assertEquals(1459695600000L, DateUtils.tsFromString("2016-04-03T15:00:00.000+00:00"));
        assertEquals(1459695600000L, DateUtils.tsFromString("2016-04-03T15:00:00.000-00:00"));
        assertEquals(1459695600123L, DateUtils.tsFromString("2016-04-03T15:00:00.123+00:00"));
        assertEquals(1459695600123L, DateUtils.tsFromString("2016-04-03T15:00:00.123-00:00"));

        // Offset times
        assertEquals(1459695600000L - 3 * 3600 * 1000, DateUtils.tsFromString("2016-04-03T15:00:00+03"));
        assertEquals(1459695600000L + 5 * 3600 * 1000, DateUtils.tsFromString("2016-04-03T15:00:00-05"));
        assertEquals(1459695600000L - 3 * 3600 * 1000, DateUtils.tsFromString("2016-04-03T15:00:00+03:00"));
        assertEquals(1459695600000L + 5 * 3600 * 1000, DateUtils.tsFromString("2016-04-03T15:00:00-05:00"));
        assertEquals(1459695600123L - 3 * 3600 * 1000, DateUtils.tsFromString("2016-04-03T15:00:00.123+03:00"));
        assertEquals(1459695600123L + 5 * 3600 * 1000, DateUtils.tsFromString("2016-04-03T15:00:00.123-05:00"));

        // Local time
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"));
        assertEquals(1459688400000L, DateUtils.tsFromString("03-APR-16 15:00:00"));
    }

    /**
     * Unit test of {@link DateUtils#tsFromString} method.
     */
    @Test(expected = UncheckedParseException.class)
    public void testTsFromStringInvalid1() {
        DateUtils.tsFromString("foobar");
    }

    /**
     * Unit test of {@link DateUtils#tsFromString} method.
     */
    @Test(expected = UncheckedParseException.class)
    public void testTsFromStringInvalid2() {
        DateUtils.tsFromString("2016/04/03");
    }

    /**
     * Unit test of {@link DateUtils#getDateFormat} method.
     */
    @Test
    public void testGetDateFormat() {
        Boolean iso = DateUtils.PROP_ISO_DATES.get();
        try {
            DateFormat f1 = DateUtils.getDateFormat(DateFormat.SHORT);
            assertNotNull(f1);
            DateUtils.PROP_ISO_DATES.put(!iso);
            DateFormat f2 = DateUtils.getDateFormat(DateFormat.SHORT);
            assertNotNull(f1);
            assertNotEquals(f1, f2);
        } finally {
            DateUtils.PROP_ISO_DATES.put(iso);
        }
    }

    /**
     * Unit test of {@link DateUtils#getTimeFormat} method.
     */
    @Test
    public void testTimeFormat() {
        Boolean iso = DateUtils.PROP_ISO_DATES.get();
        try {
            DateFormat f1 = DateUtils.getTimeFormat(DateFormat.SHORT);
            assertNotNull(f1);
            DateUtils.PROP_ISO_DATES.put(!iso);
            DateFormat f2 = DateUtils.getTimeFormat(DateFormat.SHORT);
            assertNotNull(f1);
            assertNotEquals(f1, f2);
        } finally {
            DateUtils.PROP_ISO_DATES.put(iso);
        }
    }

    @Test
    public void testCloneDate() {
        assertNull(DateUtils.cloneDate(null));
        final Date date = new Date(1453694709000L);
        assertEquals(date, DateUtils.cloneDate(date));
        assertNotSame(date, DateUtils.cloneDate(date));
    }
}
