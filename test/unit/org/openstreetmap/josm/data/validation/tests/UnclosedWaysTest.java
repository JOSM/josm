// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.mappaint.ElemStyles;
import org.openstreetmap.josm.testutils.annotations.MapPaintStyles;
import org.openstreetmap.josm.testutils.annotations.Projection;
import org.openstreetmap.josm.testutils.annotations.TaggingPresets;

/**
 * JUnit Test of unclosed ways validation test.
 */
@MapPaintStyles
@Projection
@TaggingPresets
class UnclosedWaysTest {
    private static Way createUnclosedWay(String tags, DataSet ds) {
        List<Node> nodes = new ArrayList<>();
        nodes.add(new Node(new LatLon(0, 1)));
        nodes.add(new Node(new LatLon(0, 2)));

        Way w = (Way) OsmUtils.createPrimitive("way "+tags);
        nodes.forEach(ds::addPrimitive);
        w.setNodes(nodes);
        ds.addPrimitive(w);
        return w;
    }

    /**
     * Non-regression test for bug #10469. Was in {@code MultipolygonTestTest}
     * @throws Exception if an exception occurs
     */
    @Test
    void testTicket10469() throws Exception {
        UnclosedWays uwTest = new UnclosedWays();
        uwTest.initialize();
        uwTest.startTest(null);
        DataSet ds = new DataSet();

        // Erroneous tag
        Way w = createUnclosedWay("amenity=parking", ds);
        uwTest.visit(w);
        assertTrue(ElemStyles.hasAreaElemStyle(w, false));
        assertEquals(1, uwTest.getErrors().size());

        w = createUnclosedWay("building=yes", ds);
        uwTest.visit(w);
        assertTrue(ElemStyles.hasAreaElemStyle(w, false));
        assertEquals(2, uwTest.getErrors().size());

        // Correct tag, without area style since #10601 (r7603)
        w = createUnclosedWay("aeroway=taxiway", ds);
        uwTest.visit(w);
        assertFalse(ElemStyles.hasAreaElemStyle(w, false));
        assertEquals(2, uwTest.getErrors().size());

        uwTest.endTest();
    }

    /**
     * Test to make sure the multipolygon ways are not ignored
     * See #19136, #19145
     * @throws Exception if an exception occurs
     */
    @Test
    void testWayInMultiPolygon() throws Exception {
        UnclosedWays uwTest = new UnclosedWays();
        uwTest.initialize();
        uwTest.startTest(null);
        DataSet ds = new DataSet();

        // Erroneous tag
        Way w = createUnclosedWay("natural=water", ds);
        Relation r = (Relation) OsmUtils.createPrimitive("relation type=multipolygon natural=wood");
        r.addMember(new RelationMember("inner", w));
        ds.addPrimitive(r);
        uwTest.visit(w);
        assertTrue(ElemStyles.hasAreaElemStyle(w, false));
        assertEquals(1, uwTest.getErrors().size());

        uwTest.endTest();
    }

    /**
     * Test to make sure the boundary ways are ignored when member of a boundary relation
     * See #19136, #19145
     * @throws Exception if an exception occurs
     */
    @Test
    void testWayInBoundary() throws Exception {
        UnclosedWays uwTest = new UnclosedWays();
        uwTest.initialize();
        uwTest.startTest(null);
        DataSet ds = new DataSet();

        // Erroneous tag
        Way w = createUnclosedWay("boundary=administrative", ds);
        Relation r = (Relation) OsmUtils.createPrimitive("relation type=boundary");
        r.addMember(new RelationMember("inner", w));
        ds.addPrimitive(r);
        uwTest.visit(w);
        assertFalse(ElemStyles.hasAreaElemStyle(w, false));
        assertEquals(0, uwTest.getErrors().size());

        uwTest.endTest();
    }

    /**
     * Test to make sure that amenity=* is closed.
     * See #19145
     * @throws Exception if an exception occurs
     */
    @Test
    void testAmenity() throws Exception {
        UnclosedWays uwTest = new UnclosedWays();
        uwTest.initialize();
        uwTest.startTest(null);
        DataSet ds = new DataSet();

        // Erroneous tag
        Way w = createUnclosedWay("amenity=school", ds);
        uwTest.visit(w);
        assertTrue(ElemStyles.hasAreaElemStyle(w, false));
        assertEquals(1, uwTest.getErrors().size());
        assertEquals(1103, uwTest.getErrors().iterator().next().getCode());

        uwTest.endTest();
    }

 }
