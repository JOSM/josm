// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.corrector;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.Tagged;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link ReverseWayNoTagCorrector} class.
 */
public class ReverseWayNoTagCorrectorTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Tests the {@link ReverseWayNoTagCorrector#getDirectionalTags} function
     */
    @Test
    public void testDirectionalTags() {
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
