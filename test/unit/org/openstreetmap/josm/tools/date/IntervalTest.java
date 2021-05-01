// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.date;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import java.time.Instant;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IntervalTest {

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

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
