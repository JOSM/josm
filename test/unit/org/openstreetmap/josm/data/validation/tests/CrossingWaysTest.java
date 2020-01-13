// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.validation.tests.CrossingWays.Boundaries;
import org.openstreetmap.josm.data.validation.tests.CrossingWays.SelfCrossing;
import org.openstreetmap.josm.data.validation.tests.CrossingWays.Ways;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit test of {@link CrossingWays}.
 */
public class CrossingWaysTest {

    /**
     * Setup test
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules rule = new JOSMTestRules().preferences();

    private static Way newUsableWay(String tags) {
        return TestUtils.newWay(tags, new Node(LatLon.NORTH_POLE), new Node(LatLon.ZERO));
    }

    private static void testMessage(int code, CrossingWays test, String tags1, String tags2) {
        assertEquals(code, test.createMessage(TestUtils.newWay(tags1), TestUtils.newWay(tags2)).code);
    }

    /**
     * Unit test of {@link CrossingWays#getSegments}
     */
    @Test
    public void testGetSegments() {
        List<List<WaySegment>> list = CrossingWays.getSegments(new HashMap<>(), EastNorth.ZERO, EastNorth.ZERO);
        assertEquals(1, list.size());
        assertTrue(list.get(0).isEmpty());
    }

    /**
     * Unit test of {@link CrossingWays#isCoastline}
     */
    @Test
    public void testIsCoastline() {
        assertTrue(CrossingWays.isCoastline(TestUtils.newWay("natural=water")));
        assertTrue(CrossingWays.isCoastline(TestUtils.newWay("natural=coastline")));
        assertTrue(CrossingWays.isCoastline(TestUtils.newWay("landuse=reservoir")));
        assertFalse(CrossingWays.isCoastline(TestUtils.newWay("landuse=military")));
    }

    /**
     * Unit test of {@link CrossingWays#isHighway}
     */
    @Test
    public void testIsHighway() {
        assertTrue(CrossingWays.isHighway(TestUtils.newWay("highway=motorway")));
        assertFalse(CrossingWays.isHighway(TestUtils.newWay("highway=rest_area")));
    }

    /**
     * Unit test of {@link CrossingWays#isRailway}
     */
    @Test
    public void testIsRailway() {
        assertTrue(CrossingWays.isRailway(TestUtils.newWay("railway=rail")));
        assertFalse(CrossingWays.isRailway(TestUtils.newWay("railway=subway")));
        assertFalse(CrossingWays.isRailway(TestUtils.newWay("highway=motorway")));
    }

    /**
     * Unit test of {@link CrossingWays#isSubwayOrTramOrRazed}
     */
    @Test
    public void testIsSubwayOrTramOrRazed() {
        assertTrue(CrossingWays.isSubwayOrTramOrRazed(TestUtils.newWay("railway=subway")));
        assertTrue(CrossingWays.isSubwayOrTramOrRazed(TestUtils.newWay("railway=construction construction=tram")));
        assertTrue(CrossingWays.isSubwayOrTramOrRazed(TestUtils.newWay("railway=disused disused=tram")));
        assertFalse(CrossingWays.isSubwayOrTramOrRazed(TestUtils.newWay("railway=construction")));
        assertFalse(CrossingWays.isSubwayOrTramOrRazed(TestUtils.newWay("railway=disused")));
        assertFalse(CrossingWays.isSubwayOrTramOrRazed(TestUtils.newWay("railway=rail")));
    }

    /**
     * Unit test of {@link CrossingWays#isProposedOrAbandoned}
     */
    @Test
    public void testIsProposedOrAbandoned() {
        assertTrue(CrossingWays.isProposedOrAbandoned(TestUtils.newWay("highway=proposed")));
        assertTrue(CrossingWays.isProposedOrAbandoned(TestUtils.newWay("railway=proposed")));
        assertTrue(CrossingWays.isProposedOrAbandoned(TestUtils.newWay("railway=abandoned")));
        assertFalse(CrossingWays.isProposedOrAbandoned(TestUtils.newWay("highway=motorway")));
    }

    /**
     * Unit test of {@link CrossingWays.Ways}
     */
    @Test
    public void testWays() {
        Ways test = new CrossingWays.Ways();
        // isPrimitiveUsable
        assertFalse(test.isPrimitiveUsable(newUsableWay("amenity=restaurant")));
        assertFalse(test.isPrimitiveUsable(newUsableWay("highway=proposed")));
        assertFalse(test.isPrimitiveUsable(TestUtils.newWay("highway=motorway"))); // Unusable (0 node)
        assertTrue(test.isPrimitiveUsable(newUsableWay("highway=motorway"))); // Usable (2 nodes)
        assertTrue(test.isPrimitiveUsable(newUsableWay("waterway=river")));
        assertTrue(test.isPrimitiveUsable(newUsableWay("railway=rail")));
        assertTrue(test.isPrimitiveUsable(newUsableWay("natural=water")));
        assertTrue(test.isPrimitiveUsable(newUsableWay("building=yes")));
        assertTrue(test.isPrimitiveUsable(newUsableWay("landuse=residential")));
        // createMessage
        testMessage(601, test, "amenity=restaurant", "amenity=restaurant");
        testMessage(610, test, "building=yes", "building=yes");
        testMessage(611, test, "building=yes", "amenity=restaurant");
        testMessage(612, test, "building=yes", "highway=road");
        testMessage(613, test, "building=yes", "railway=rail");
        testMessage(614, test, "building=yes", "landuse=residential");
        testMessage(615, test, "building=yes", "waterway=river");
        testMessage(620, test, "highway=road", "highway=road");
        testMessage(621, test, "highway=road", "amenity=restaurant");
        testMessage(622, test, "highway=road", "railway=rail");
        testMessage(623, test, "highway=road", "waterway=river");
        testMessage(630, test, "railway=rail", "railway=rail");
        testMessage(631, test, "railway=rail", "amenity=restaurant");
        testMessage(632, test, "railway=rail", "waterway=river");
        testMessage(640, test, "landuse=residential", "landuse=residential");
        testMessage(641, test, "landuse=residential", "amenity=restaurant");
        testMessage(650, test, "waterway=river", "waterway=river");
        testMessage(651, test, "waterway=river", "amenity=restaurant");
        testMessage(603, test, "barrier=hedge", "barrier=yes");
        testMessage(661, test, "barrier=hedge", "building=yes");
        testMessage(662, test, "barrier=hedge", "highway=road");
        testMessage(663, test, "barrier=hedge", "railway=rail");
        testMessage(664, test, "barrier=hedge", "waterway=river");

        assertFalse(test.isPrimitiveUsable(newUsableWay("amenity=restaurant")));
        assertFalse(test.isPrimitiveUsable(TestUtils.newWay("barrier=yes"))); // Unusable (0 node)
        assertTrue(test.isPrimitiveUsable(newUsableWay("barrier=yes"))); // Usable (2 nodes)

    }

    /**
     * Unit test of {@link CrossingWays.Boundaries}
     */
    @Test
    public void testBoundaries() {
        Boundaries test = new CrossingWays.Boundaries();
        // isPrimitiveUsable
        assertFalse(test.isPrimitiveUsable(newUsableWay("amenity=restaurant")));
        assertFalse(test.isPrimitiveUsable(TestUtils.newWay("boudary=administrative"))); // Unusable (0 node)
        assertTrue(test.isPrimitiveUsable(newUsableWay("boundary=administrative"))); // Usable (2 nodes)
        assertFalse(test.isPrimitiveUsable(TestUtils.newRelation("boundary=administrative")));
        assertTrue(test.isPrimitiveUsable(TestUtils.newRelation("boundary=administrative type=multipolygon")));
    }

    /**
     * Unit test of {@link CrossingWays.SelfCrossing}
     */
    @Test
    public void testSelfCrossing() {
        SelfCrossing test = new CrossingWays.SelfCrossing();
        // isPrimitiveUsable
        assertFalse(test.isPrimitiveUsable(newUsableWay("highway=motorway")));
        assertFalse(test.isPrimitiveUsable(newUsableWay("barrier=yes")));
        assertFalse(test.isPrimitiveUsable(newUsableWay("boundary=administrative")));
        assertFalse(test.isPrimitiveUsable(TestUtils.newWay("amenity=restaurant"))); // Unusable (0 node)
        assertTrue(test.isPrimitiveUsable(newUsableWay("amenity=restaurant"))); // Usable (2 nodes)
    }
}
