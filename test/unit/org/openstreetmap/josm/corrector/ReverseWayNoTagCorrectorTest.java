// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.corrector;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.osm.Tag;

/**
 * Unit tests of {@link ReverseWayNoTagCorrector} class.
 */
public class ReverseWayNoTagCorrectorTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Tests the {@link ReverseWayNoTagCorrector#getDirectionalTags} function
     */
    @Test
    public void testDirectionalTags() {
        assertEquals(1, ReverseWayNoTagCorrector.getDirectionalTags(new Tag("waterway", "drain")).size());
        assertEquals(1, ReverseWayNoTagCorrector.getDirectionalTags(new Tag("man_made", "embankment")).size());
        assertEquals(1, ReverseWayNoTagCorrector.getDirectionalTags(new Tag("aerialway", "cable_car")).size());
        assertEquals(0, ReverseWayNoTagCorrector.getDirectionalTags(new Tag("aerialway", "station")).size());
        assertEquals(0, ReverseWayNoTagCorrector.getDirectionalTags(new Tag("incline", "up")).size());
        assertEquals(0, ReverseWayNoTagCorrector.getDirectionalTags(new Tag("oneway", "yes")).size());
    }
}
