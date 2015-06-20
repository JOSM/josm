// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.mappaint.ElemStyles;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;

/**
 * JUnit Test of Multipolygon validation test.
 */
public class MultipolygonTestTest {

    private static final MultipolygonTest MULTIPOLYGON_TEST = new MultipolygonTest();

    /**
     * Setup test.
     * @throws Exception if test cannot be initialized
     */
    @Before
    public void setUp() throws Exception {
        JOSMFixture.createUnitTestFixture().init();
        MapPaintStyles.readFromPreferences();
        MULTIPOLYGON_TEST.initialize();
    }

    private static Way createUnclosedWay(String tags) {
        List<Node> nodes = new ArrayList<>();
        nodes.add(new Node(new LatLon(0, 1)));
        nodes.add(new Node(new LatLon(0, 2)));

        Way w = (Way) OsmUtils.createPrimitive("way "+tags);
        w.setNodes(nodes);
        return w;
    }

    /**
     * Non-regression test for bug #10469.
     */
    @Test
    public void testTicket10469() {
        MULTIPOLYGON_TEST.startTest(null);

        List<Node> nodes = new ArrayList<>();
        nodes.add(new Node(new LatLon(0, 1)));
        nodes.add(new Node(new LatLon(0, 2)));

        // Erroneous tag
        Way w = createUnclosedWay("amenity=parking");
        MULTIPOLYGON_TEST.visit(w);
        assertTrue(ElemStyles.hasAreaElemStyle(w, false));
        assertEquals(1, MULTIPOLYGON_TEST.getErrors().size());

        // Erroneous tag, but managed by another test
        w = createUnclosedWay("building=yes");
        MULTIPOLYGON_TEST.visit(w);
        assertTrue(ElemStyles.hasAreaElemStyle(w, false));
        assertEquals(1, MULTIPOLYGON_TEST.getErrors().size());

        // Correct tag, without area style since #10601 (r7603)
        w = createUnclosedWay("aeroway=taxiway");
        MULTIPOLYGON_TEST.visit(w);
        assertFalse(ElemStyles.hasAreaElemStyle(w, false));
        assertEquals(1, MULTIPOLYGON_TEST.getErrors().size());

        MULTIPOLYGON_TEST.endTest();
    }
}
