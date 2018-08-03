// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

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
 * Unit tests of {@link Offset} class.
 */
public class OffsetTest {

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
     * Unit test of {@link Offset#formatOffset}.
     */
    @Test
    public void testFormatOffset() {
        assertEquals("0", Offset.seconds(0).formatOffset());
        assertEquals("123", Offset.seconds(123).formatOffset());
        assertEquals("-4242", Offset.seconds(-4242).formatOffset());
        assertEquals("0.1", Offset.milliseconds(100).formatOffset());
        assertEquals("0.120", Offset.milliseconds(120).formatOffset());
        assertEquals("0.123", Offset.milliseconds(123).formatOffset());
        assertEquals("1.2", Offset.milliseconds(1200).formatOffset());
        assertEquals("1.234", Offset.milliseconds(1234).formatOffset());
    }

    /**
     * Unit test of {@link Offset#parseOffset}.
     * @throws ParseException in case of parsing error
     */
    @Test
    public void testParseOffest() throws ParseException {
        assertEquals(0, Offset.parseOffset("0").getSeconds());
        assertEquals(4242L, Offset.parseOffset("4242").getSeconds());
        assertEquals(-4242L, Offset.parseOffset("-4242").getSeconds());
        assertEquals(0L, Offset.parseOffset("-0").getSeconds());
        assertEquals(100L, Offset.parseOffset("0.1").getMilliseconds());
        assertEquals(123L, Offset.parseOffset("0.123").getMilliseconds());
        assertEquals(-42420L, Offset.parseOffset("-42.42").getMilliseconds());
    }

    /**
     * Unit test of {@link Offset#splitOutTimezone}.
     */
    @Test
    public void testSplitOutTimezone() {
        assertEquals("+1:00", Offset.seconds(3602).splitOutTimezone().a.formatTimezone());
        assertEquals("2", Offset.seconds(3602).splitOutTimezone().b.formatOffset());
        assertEquals("-7:00", Offset.seconds(-7 * 3600 + 123).splitOutTimezone().a.formatTimezone());
        assertEquals("123", Offset.seconds(-7 * 3600 + 123).splitOutTimezone().b.formatOffset());
        assertEquals(1, Offset.seconds(35 * 3600 + 421).getDayOffset());
        assertEquals(11 * 3600 + 421, Offset.seconds(35 * 3600 + 421).withoutDayOffset().getSeconds());
        assertEquals("+11:00", Offset.seconds(35 * 3600 + 421).splitOutTimezone().a.formatTimezone());
        assertEquals(86400 + 421, Offset.seconds(35 * 3600 + 421).splitOutTimezone().b.getSeconds());
        assertEquals(421, Offset.seconds(35 * 3600 + 421).withoutDayOffset().splitOutTimezone().b.getSeconds());
        assertEquals("+1:00", Offset.milliseconds(3602987).splitOutTimezone().a.formatTimezone());
        assertEquals("2.987", Offset.milliseconds(3602987).splitOutTimezone().b.formatOffset());
    }
}
