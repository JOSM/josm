// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.corrector;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.Tagged;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests of {@link ReverseWayNoTagCorrector} class.
 */
@BasicPreferences
class ReverseWayNoTagCorrectorTest {
    /**
     * Tests the {@link ReverseWayNoTagCorrector#getDirectionalTags} function
     */
    @Test
    void testDirectionalTags() {
        assertEquals(1, ReverseWayNoTagCorrector.getDirectionalTags(new Tag("waterway", "drain")).size());
        assertEquals(1, ReverseWayNoTagCorrector.getDirectionalTags(new Tag("man_made", "embankment")).size());
        assertEquals(1, ReverseWayNoTagCorrector.getDirectionalTags(new Tag("aerialway", "drag_lift")).size());
        assertEquals(0, ReverseWayNoTagCorrector.getDirectionalTags(new Tag("aerialway", "station")).size());
        assertEquals(0, ReverseWayNoTagCorrector.getDirectionalTags(new Tag("incline", "up")).size());
        assertEquals(0, ReverseWayNoTagCorrector.getDirectionalTags(new Tag("oneway", "yes")).size());
        assertEquals(1, ReverseWayNoTagCorrector.getDirectionalTags(new Tag("barrier", "kerb")).size());
        assertEquals(1, ReverseWayNoTagCorrector.getDirectionalTags(new Tag("barrier", "city_wall")).size());

        final Tagged twoSidedCityWall = new Way();
        twoSidedCityWall.put("barrier", "city_wall");
        twoSidedCityWall.put("two_sided", "yes");
        assertEquals(0, ReverseWayNoTagCorrector.getDirectionalTags(twoSidedCityWall).size());
    }
}
