// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import static org.junit.Assert.assertEquals;

import java.text.ParseException;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.date.DateUtils;
import org.openstreetmap.josm.tools.date.DateUtilsTest;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link GpxTimeOffset} class.
 */
public class GpxOffsetTest {

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
     * Unit test of {@link GpxTimeOffset#formatOffset}.
     */
    @Test
    public void testFormatOffset() {
        assertEquals("0", GpxTimeOffset.seconds(0).formatOffset());
        assertEquals("123", GpxTimeOffset.seconds(123).formatOffset());
        assertEquals("-4242", GpxTimeOffset.seconds(-4242).formatOffset());
        assertEquals("0.1", GpxTimeOffset.milliseconds(100).formatOffset());
        assertEquals("0.120", GpxTimeOffset.milliseconds(120).formatOffset());
        assertEquals("0.123", GpxTimeOffset.milliseconds(123).formatOffset());
        assertEquals("1.2", GpxTimeOffset.milliseconds(1200).formatOffset());
        assertEquals("1.234", GpxTimeOffset.milliseconds(1234).formatOffset());
    }

    /**
     * Unit test of {@link GpxTimeOffset#parseOffset}.
     * @throws ParseException in case of parsing error
     */
    @Test
    public void testParseOffest() throws ParseException {
        assertEquals(0, GpxTimeOffset.parseOffset("0").getSeconds());
        assertEquals(4242L, GpxTimeOffset.parseOffset("4242").getSeconds());
        assertEquals(-4242L, GpxTimeOffset.parseOffset("-4242").getSeconds());
        assertEquals(0L, GpxTimeOffset.parseOffset("-0").getSeconds());
        assertEquals(100L, GpxTimeOffset.parseOffset("0.1").getMilliseconds());
        assertEquals(123L, GpxTimeOffset.parseOffset("0.123").getMilliseconds());
        assertEquals(-42420L, GpxTimeOffset.parseOffset("-42.42").getMilliseconds());
    }

    /**
     * Unit test of {@link GpxTimeOffset#splitOutTimezone}.
     */
    @Test
    public void testSplitOutTimezone() {
        assertEquals("+1:00", GpxTimeOffset.seconds(3602).splitOutTimezone().a.formatTimezone());
        assertEquals("2", GpxTimeOffset.seconds(3602).splitOutTimezone().b.formatOffset());
        assertEquals("-7:00", GpxTimeOffset.seconds(-7 * 3600 + 123).splitOutTimezone().a.formatTimezone());
        assertEquals("123", GpxTimeOffset.seconds(-7 * 3600 + 123).splitOutTimezone().b.formatOffset());
        assertEquals(1, GpxTimeOffset.seconds(35 * 3600 + 421).getDayOffset());
        assertEquals(11 * 3600 + 421, GpxTimeOffset.seconds(35 * 3600 + 421).withoutDayOffset().getSeconds());
        assertEquals("+11:00", GpxTimeOffset.seconds(35 * 3600 + 421).splitOutTimezone().a.formatTimezone());
        assertEquals(86400 + 421, GpxTimeOffset.seconds(35 * 3600 + 421).splitOutTimezone().b.getSeconds());
        assertEquals(421, GpxTimeOffset.seconds(35 * 3600 + 421).withoutDayOffset().splitOutTimezone().b.getSeconds());
        assertEquals("+1:00", GpxTimeOffset.milliseconds(3602987).splitOutTimezone().a.formatTimezone());
        assertEquals("2.987", GpxTimeOffset.milliseconds(3602987).splitOutTimezone().b.formatOffset());
    }
}
