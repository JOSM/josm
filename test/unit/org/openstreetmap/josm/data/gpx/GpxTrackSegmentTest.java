// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests for class {@link GpxTrackSegment}.
 */
public class GpxTrackSegmentTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Unit test of methods {@link GpxTrackSegment#equals} and {@link GpxTrackSegment#hashCode}.
     */
    @Test
    public void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        GpxExtensionCollection col = new GpxExtensionCollection();
        col.add("josm", "from-server", "true");
        EqualsVerifier.forClass(GpxTrackSegment.class).usingGetClass()
            .suppress(Warning.NONFINAL_FIELDS)
            .withIgnoredFields("bounds", "length")
            .withPrefabValues(WayPoint.class, new WayPoint(LatLon.NORTH_POLE), new WayPoint(LatLon.SOUTH_POLE))
            .withPrefabValues(GpxExtensionCollection.class, new GpxExtensionCollection(), col)
            .verify();
    }
}
