// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Unit tests for class {@link WayPoint}.
 */
class WayPointTest {

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Unit test of methods {@link WayPoint#equals} and {@link WayPoint#hashCode}.
     */
    @Test
    void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        GpxExtensionCollection col = new GpxExtensionCollection();
        col.add("josm", "from-server", "true");
        EqualsVerifier.forClass(WayPoint.class).usingGetClass()
            .suppress(Warning.NONFINAL_FIELDS)
            .withIgnoredFields("customColoring", "dir", "drawLine", "east", "north", "eastNorthCacheKey")
            .withPrefabValues(GpxExtensionCollection.class, new GpxExtensionCollection(), col)
            .verify();
    }

    /**
     * Unit test of copy constructor {@link WayPoint#WayPoint(WayPoint)}
     */
    @Test
    void testConstructor() {
        WayPoint wp1 = new WayPoint(new LatLon(12., 34.));
        wp1.setInstant(Instant.ofEpochMilli(123_456_789));
        WayPoint wp2 = new WayPoint(wp1);
        assertEquals(wp1, wp2);
        assertEquals(wp1.getInstant(), wp2.getInstant());
        wp2.setInstant(Instant.ofEpochMilli(234_456_789));
        assertNotEquals(wp1, wp2);
        assertNotEquals(wp1.getInstant(), wp2.getInstant());
    }
}
