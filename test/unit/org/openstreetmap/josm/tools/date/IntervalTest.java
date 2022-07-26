// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.date;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.I18n;
import org.openstreetmap.josm.testutils.annotations.TimeZoneAnnotation;

@BasicPreferences
@I18n
@TimeZoneAnnotation
class IntervalTest {
    /**
     * Setup test.
     */
    @BeforeEach
    void setUp() {
        Locale.setDefault(Locale.ROOT);
        DateUtils.PROP_ISO_DATES.put(true);
    }

    /**
     * Unit test of {@link Interval#format}.
     */
    @Test
    void testFormat() {
        Interval interval = new Interval(Instant.parse("2021-03-01T17:53:16Z"), Instant.parse("2021-04-03T08:19:19Z"));
        assertEquals("2021-03-01 17:53:16 \u2013 2021-04-03 08:19:19 (32 days 14 h)", interval.format());
    }

    /**
     * Unit test of {@link Interval#toString}.
     */
    @Test
    void testToString() {
        Interval interval = new Interval(Instant.parse("2021-03-01T17:53:16Z"), Instant.parse("2021-04-03T08:19:19Z"));
        assertEquals("2021-03-01T17:53:16Z/2021-04-03T08:19:19Z", interval.toString());
    }

}
