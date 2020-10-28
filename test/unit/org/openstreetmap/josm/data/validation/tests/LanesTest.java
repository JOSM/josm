// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.osm.OsmUtils;

/**
 * Unit tests of {@link Lanes}.
 */
class LanesTest {

    private final Lanes lanes = new Lanes();

    /**
     * Setup test.
     * @throws Exception if an error occurs
     */
    @BeforeEach
    public void setUp() throws Exception {
        JOSMFixture.createUnitTestFixture().init();
        lanes.initialize();
        lanes.startTest(null);
    }

    /**
     * Unit test of {@link Lanes#getLanesCount}.
     */
    @Test
    void testLanesCount() {
        assertEquals(0, Lanes.getLanesCount(""));
        assertEquals(1, Lanes.getLanesCount("left"));
        assertEquals(2, Lanes.getLanesCount("left|right"));
        assertEquals(3, Lanes.getLanesCount("yes|no|yes"));
        assertEquals(3, Lanes.getLanesCount("yes||"));
    }

    @Test
    void test1() {
        lanes.check(OsmUtils.createPrimitive("way turn:lanes=left|right change:lanes=only_left|not_right|yes"));
        assertEquals("Number of lane dependent values inconsistent", lanes.getErrors().get(0).getMessage());
    }

    @Test
    void test2() {
        lanes.check(OsmUtils.createPrimitive("way width:lanes:forward=1|2|3 psv:lanes:forward=no|designated"));
        assertEquals("Number of lane dependent values inconsistent in forward direction", lanes.getErrors().get(0).getMessage());
    }

    @Test
    void test3() {
        lanes.check(OsmUtils.createPrimitive("way change:lanes:forward=yes|no turn:lanes:backward=left|right|left"));
        assertTrue(lanes.getErrors().isEmpty());
    }

    @Test
    void test4() {
        lanes.check(OsmUtils.createPrimitive("way turn:lanes:forward=left|right change:lanes:forward=yes|no|yes width:backward=1|2|3"));
        assertEquals("Number of lane dependent values inconsistent in forward direction", lanes.getErrors().get(0).getMessage());
    }

    @Test
    void test5() {
        lanes.check(OsmUtils.createPrimitive("way lanes:forward=5 turn:lanes:forward=left|right"));
        assertEquals("Number of lanes:forward greater than *:lanes:forward", lanes.getErrors().get(0).getMessage());
    }

    @Test
    void test6() {
        lanes.check(OsmUtils.createPrimitive("way lanes:forward=foo|bar turn:lanes:forward=foo+bar"));
        assertTrue(lanes.getErrors().isEmpty());
    }

    @Test
    void test7() {
        lanes.check(OsmUtils.createPrimitive("way lanes=3 lanes:forward=3 lanes:backward=7"));
        assertEquals("Number of lanes:forward+lanes:backward greater than lanes", lanes.getErrors().get(0).getMessage());
    }

    @Test
    void test8() {
        lanes.check(OsmUtils.createPrimitive(
                "way destination:country:lanes=X|Y;Z|none destination:ref:lanes=xyz|| destination:sign:lanes=none|airport|none"));
        assertTrue(lanes.getErrors().isEmpty());
    }

    @Test
    void test9() {
        lanes.check(OsmUtils.createPrimitive("way highway=secondary lanes=2 source:lanes=survey"));
        assertTrue(lanes.getErrors().isEmpty());
    }
}
