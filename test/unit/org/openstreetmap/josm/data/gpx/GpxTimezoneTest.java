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
 * Unit tests of {@link GpxTimezone} class.
 */
public class GpxTimezoneTest {

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
     * Unit test of {@link GpxTimezone#formatTimezone}.
     */
    @Test
    public void testFormatTimezone() {
        assertEquals("+1:00", new GpxTimezone(1).formatTimezone());
        assertEquals("+6:30", new GpxTimezone(6.5).formatTimezone());
        assertEquals("-6:30", new GpxTimezone(-6.5).formatTimezone());
        assertEquals("+3:08", new GpxTimezone(Math.PI).formatTimezone());
        assertEquals("+2:43", new GpxTimezone(Math.E).formatTimezone());
    }

    /**
     * Unit test of {@link GpxTimezone#parseTimezone}.
     * @throws ParseException in case of parsing error
     */
    @Test
    public void testParseTimezone() throws ParseException {
        assertEquals(1, GpxTimezone.parseTimezone("+01:00").getHours(), 1e-3);
        assertEquals(1, GpxTimezone.parseTimezone("+1:00").getHours(), 1e-3);
        assertEquals(1.5, GpxTimezone.parseTimezone("+01:30").getHours(), 1e-3);
        assertEquals(11.5, GpxTimezone.parseTimezone("+11:30").getHours(), 1e-3);
        assertEquals(-11.5, GpxTimezone.parseTimezone("-11:30").getHours(), 1e-3);
    }
}
