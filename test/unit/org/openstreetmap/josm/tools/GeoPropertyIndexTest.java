// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.tools.GeoPropertyIndex.GPLevel;

/**
 * Unit tests of {@link GeoPropertyIndex} class.
 */
class GeoPropertyIndexTest {

    @Test
    void testIsInside() {
        assertFalse(new GPLevel<>(0,
                new BBox(119.53125, 30.234375, 120.9375, 30.9375), null,
                new GeoPropertyIndex<>(new DefaultGeoProperty(Collections.emptyList()), 24))
            .isInside(new LatLon(30.580878544754302, 119.53124999999997)));
    }
}
