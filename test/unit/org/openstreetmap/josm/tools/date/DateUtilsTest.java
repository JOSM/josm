// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.text.DateFormat;
import java.time.Instant;
import java.time.format.FormatStyle;
import java.util.Date;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.ForkJoinPool;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.I18n;
import org.openstreetmap.josm.tools.UncheckedParseException;

import net.trajano.commons.testing.UtilityClassTestUtil;

/**
 * Unit tests of {@link DateUtils} class.
 */
@BasicPreferences
@I18n
public class DateUtilsTest {
    /**
     * Tests that {@code DateUtils} satisfies utility class criteria.
     * @throws ReflectiveOperationException if an error occurs
     */
    @Test
    void testUtilityClass() throws ReflectiveOperationException {
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
    void testMapDate() {
        assertEquals(1344870637000L, DateUtils.fromString("2012-08-13T15:10:37Z").getTime());
    }

    /**
     * Test to parse date as returned for note data.
     */
    @Test
    void testNoteDate() {
        assertEquals(1417298930000L, DateUtils.fromString("2014-11-29 22:08:50 UTC").getTime());
    }

    /**
     * Test to parse date as used in EXIF structures.
     */
    @Test
    void testExifDate() {
        assertEquals(1443038712000L, DateUtils.fromString("2015:09:23 20:05:12").getTime());
        assertEquals(1443038712888L, DateUtils.fromString("2015:09:23 20:05:12.888").getTime());
    }

    /**
     * Test to parse date as used in GPX files
     */
    @Test
    void testGPXDate() {
        assertEquals(1277465405000L, DateUtils.fromString("2010-06-25T11:30:05.000Z").getTime());
    }

    /**
     * Test to parse date as defined in <a href="https://tools.ietf.org/html/rfc3339">RFC 3339</a>
     */
    @Test
    void testRfc3339() {
        // examples taken from RFC
        assertEquals(482196050520L, DateUtils.fromString("1985-04-12T23:20:50.52Z").getTime());
        assertEquals(851042397000L, DateUtils.fromString("1996-12-19T16:39:57-08:00").getTime());
        assertEquals(-1041337172130L, DateUtils.fromString("1937-01-01T12:00:27.87+00:20").getTime());
        // (partial) dates
        assertEquals(482112000000L, DateUtils.fromString("1985-04-12").getTime());
        assertEquals(481161600000L, DateUtils.fromString("1985-04").getTime());
        assertEquals(473385600000L, DateUtils.fromString("1985").getTime());
    }

    @Test
    void testRtklib() {
        // examples taken from rtklib .pos files
        assertEquals("2019-04-21T08:20:32Z", DateUtils.parseInstant("2019/04/21 08:20:32").toString());
        assertEquals("2019-06-08T08:23:12.123Z", DateUtils.parseInstant("2019/06/08 08:23:12.123").toString());
        assertEquals("2021-03-30T15:04:01.123456Z", DateUtils.parseInstant("2021/03/30 15:04:01.123456").toString());
    }

    /**
     * Verifies that parsing an illegal date throws a {@link UncheckedParseException}
     */
    @ParameterizedTest
    @ValueSource(strings = {"2014-", "2014-01-", "2014-01-01T", "2014-00-01", "2014-01-00"})
    void testIllegalDate(String date) {
        assertThrows(UncheckedParseException.class, () -> DateUtils.fromString(date));
    }

    /**
     * Tests that formatting a date w/ milliseconds does not cause incorrect parsing afterwards
     */
    @Test
    void testFormattingMillisecondsDoesNotCauseIncorrectParsing() {
        DateUtils.fromDate(new Date(123));
        assertEquals(1453694709000L, DateUtils.fromString("2016-01-25T04:05:09.000Z").getTime());
        assertEquals(1453694709200L, DateUtils.fromString("2016-01-25T04:05:09.200Z").getTime());
        assertEquals(1453694709400L, DateUtils.fromString("2016-01-25T04:05:09.400Z").getTime());
    }

    /**
     * Unit test of {@link DateUtils#fromTimestamp} method.
     */
    @Test
    void testFromTimestamp() {
        assertEquals("1970-01-01T00:00:00Z", DateUtils.fromTimestamp(0));
        assertEquals("2001-09-09T01:46:40Z", DateUtils.fromTimestamp(1000000000));
        assertEquals("2038-01-19T03:14:07Z", DateUtils.fromTimestamp(Integer.MAX_VALUE));
    }

    /**
     * Unit test of {@link DateUtils#fromDate} method.
     */
    @Test
    void testFromDate() {
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
    void testFormatTime() {
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
    void testFormatDate() {
        assertEquals("1/1/70", DateUtils.formatDate(new Date(123), DateFormat.SHORT));
        assertEquals("January 1, 1970", DateUtils.formatDate(new Date(123), DateFormat.LONG));
    }

    /**
     * Unit test of {@link DateUtils#tsFromString} method.
     */
    @Test
    void testTsFromString() {
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
        setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));
        assertEquals(1459688400000L, DateUtils.tsFromString("03-APR-16 15:00:00"));
    }

    @Test
    @Disabled("slow; use for thread safety testing")
    void testTsFromString800k() throws Exception {
        new ForkJoinPool(64).submit(() -> new Random()
                .longs(800_000)
                .parallel()
                .forEach(ignore -> testTsFromString())).get();
    }

    /**
     * Unit test of {@link DateUtils#tsFromString} method.
     */
    @Test
    void testTsFromStringInvalid1() {
        assertThrows(UncheckedParseException.class, () -> DateUtils.tsFromString("foobar"));
    }

    /**
     * Unit test of {@link DateUtils#tsFromString} method.
     */
    @Test
    void testTsFromStringInvalid2() {
        assertThrows(UncheckedParseException.class, () -> DateUtils.tsFromString("2016/04/03"));
    }

    /**
     * Unit test of {@link DateUtils#getDateFormat} method.
     */
    @Test
    void testGetDateFormat() {
        Boolean iso = DateUtils.PROP_ISO_DATES.get();
        try {
            DateFormat f1 = DateUtils.getDateFormat(DateFormat.SHORT);
            assertNotNull(f1);
            DateUtils.PROP_ISO_DATES.put(!iso);
            DateFormat f2 = DateUtils.getDateFormat(DateFormat.SHORT);
            assertNotNull(f1);
            assertNotEquals(f1, f2);
            DateUtils.PROP_ISO_DATES.put(true);
            assertEquals("2006-01-02", DateUtils.getDateFormatter(null).format(Instant.parse("2006-01-02T15:04:05.777Z")));
        } finally {
            DateUtils.PROP_ISO_DATES.put(iso);
        }
    }

    /**
     * Unit test of {@link DateUtils#getTimeFormat} method.
     */
    @Test
    void testTimeFormat() {
        Boolean iso = DateUtils.PROP_ISO_DATES.get();
        try {
            DateFormat f1 = DateUtils.getTimeFormat(DateFormat.SHORT);
            assertNotNull(f1);
            DateUtils.PROP_ISO_DATES.put(!iso);
            DateFormat f2 = DateUtils.getTimeFormat(DateFormat.SHORT);
            assertNotNull(f1);
            assertNotEquals(f1, f2);
            DateUtils.PROP_ISO_DATES.put(true);
            assertEquals("15:04:05.777", DateUtils.getTimeFormatter(null).format(Instant.parse("2006-01-02T15:04:05.777Z")));
            assertEquals("15:04:05", DateUtils.getTimeFormatter(null).format(Instant.parse("2006-01-02T15:04:05Z")));
            assertEquals("15:04:00", DateUtils.getTimeFormatter(null).format(Instant.parse("2006-01-02T15:04:00Z")));
        } finally {
            DateUtils.PROP_ISO_DATES.put(iso);
        }
    }

    @Test
    void testCloneDate() {
        assertNull(DateUtils.cloneDate(null));
        final Date date = new Date(1453694709000L);
        assertEquals(date, DateUtils.cloneDate(date));
        assertNotSame(date, DateUtils.cloneDate(date));
    }

    /**
     * Unit test of {@link DateUtils#getDateTimeFormatter} method.
     */
    @Test
    void testDateTimeFormatter() {
        Instant instant = Instant.parse("2006-01-02T15:04:05.777Z");
        Boolean iso = DateUtils.PROP_ISO_DATES.get();
        try {
            assertNotNull(DateUtils.getDateFormatter(FormatStyle.SHORT).format(instant));
            assertNotNull(DateUtils.getTimeFormatter(FormatStyle.SHORT).format(instant));
            assertNotNull(DateUtils.getDateTimeFormatter(FormatStyle.SHORT, FormatStyle.SHORT).format(instant));
            DateUtils.PROP_ISO_DATES.put(!iso);
            assertNotNull(DateUtils.getDateFormatter(FormatStyle.SHORT).format(instant));
            assertNotNull(DateUtils.getTimeFormatter(FormatStyle.SHORT).format(instant));
            assertNotNull(DateUtils.getDateTimeFormatter(FormatStyle.SHORT, FormatStyle.SHORT).format(instant));
            DateUtils.PROP_ISO_DATES.put(true);
            assertEquals("2006-01-02 15:04:05.777", DateUtils.getDateTimeFormatter(null, null).format(instant));
            assertEquals(Instant.parse("2006-01-02T15:04:05.000Z"),
                    DateUtils.getDateTimeFormatter(null, null).parse("2006-01-02 15:04:05", Instant::from));
            assertEquals(Instant.parse("2006-01-02T15:04:00.000Z"),
                    DateUtils.getDateTimeFormatter(null, null).parse("2006-01-02 15:04", Instant::from));
        } finally {
            DateUtils.PROP_ISO_DATES.put(iso);
        }
    }
}
