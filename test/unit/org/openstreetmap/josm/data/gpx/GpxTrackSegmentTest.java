// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests for class {@link GpxTrackSegment}.
 */
class GpxTrackSegmentTest {
    /**
     * Unit test of methods {@link GpxTrackSegment#equals} and {@link GpxTrackSegment#hashCode}.
     */
    @Test
    void testEqualsContract() {
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
