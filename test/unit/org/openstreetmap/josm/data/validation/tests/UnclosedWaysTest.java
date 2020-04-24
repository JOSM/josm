// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.mappaint.ElemStyles;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * JUnit Test of unclosed ways validation test.
 */
public class UnclosedWaysTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().projection().mapStyles().presets();

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
    public void testTicket10469() throws Exception {
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
    public void testWayInMultiPolygon() throws Exception {
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
    public void testWayInBoundary() throws Exception {
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
    public void testAmenity() throws Exception {
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
