// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class RelationTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    @Test(expected = NullPointerException.class)
    public void testCreateNewRelation() {
        new Relation(null);
    }

    @Test
    public void testEqualSemanticsToNull() {
        Relation relation = new Relation();
        assertFalse(relation.hasEqualTechnicalAttributes(null));
    }

    @Test
    public void testBbox() {
        DataSet ds = new DataSet();

        Node n1 = new Node(new LatLon(10, 10));
        Node n2 = new Node(new LatLon(20, 20));
        Node n3 = new Node(new LatLon(30, 30));
        Way w1 = new Way();
        w1.addNode(n1);
        w1.addNode(n2);
        Relation r1 = new Relation();
        Relation r2 = new Relation();
        ds.addPrimitive(r1);
        ds.addPrimitive(r2);
        ds.addPrimitive(n1);
        ds.addPrimitive(n2);
        ds.addPrimitive(n3);
        ds.addPrimitive(w1);
        r1.addMember(new RelationMember("", n1));
        r1.addMember(new RelationMember("", w1));
        r1.addMember(new RelationMember("", r1));
        r1.addMember(new RelationMember("", r2));
        r2.addMember(new RelationMember("", r1));
        r2.addMember(new RelationMember("", n3));

        BBox bbox = new BBox(w1);
        bbox.add(n3.getBBox());
        Assert.assertEquals(bbox, r1.getBBox());
        Assert.assertEquals(bbox, r2.getBBox());

        n3.setCoor(new LatLon(40, 40));
        bbox.add(n3.getBBox());
        Assert.assertEquals(bbox, r1.getBBox());
        Assert.assertEquals(bbox, r2.getBBox());

        r1.removeMembersFor(r2);
        Assert.assertEquals(w1.getBBox(), r1.getBBox());
        Assert.assertEquals(bbox, r2.getBBox());

        w1.addNode(n3);
        Assert.assertEquals(w1.getBBox(), r1.getBBox());
        Assert.assertEquals(w1.getBBox(), r2.getBBox());

        // create incomplete node and add it to the relation, this must not change the bbox
        BBox oldBBox = r2.getBBox();
        Node n4 = new Node();
        n4.setIncomplete(true);
        ds.addPrimitive(n4);
        r2.addMember(new RelationMember("", n4));

        Assert.assertEquals(oldBBox, r2.getBBox());
    }

    @Test
    public void testBBoxNotInDataset() {
        Node n1 = new Node(new LatLon(10, 10));
        Node n2 = new Node(new LatLon(20, 20));
        Way w1 = new Way();
        w1.addNode(n1);
        w1.addNode(n2);
        Relation r1 = new Relation();
        r1.getBBox();
        r1.addMember(new RelationMember("", w1));

        Assert.assertEquals(new BBox(w1), r1.getBBox());

        DataSet ds = new DataSet();
        ds.addPrimitive(n1);
        ds.addPrimitive(n2);
        ds.addPrimitive(w1);
        ds.addPrimitive(r1);

        Assert.assertEquals(new BBox(w1), r1.getBBox());

        ds.removePrimitive(r1);

        n1.setCoor(new LatLon(30, 40));
        Assert.assertEquals(new BBox(w1), r1.getBBox());

        ds.addPrimitive(r1);
        Assert.assertEquals(new BBox(w1), r1.getBBox());
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/12467">Bug #12467</a>.
     * @throws Exception if any error occurs
     */
    @Test
    public void testTicket12467() throws Exception {
        Relation r = new Relation();
        r.put("type", "boundary");
        assertTrue(r.isBoundary());
        assertTrue(r.isMultipolygon());
        assertEquals(OsmPrimitiveType.RELATION, r.getDisplayType());

        r.put("type", "multipolygon");
        assertFalse(r.isBoundary());
        assertTrue(r.isMultipolygon());
        assertEquals(OsmPrimitiveType.MULTIPOLYGON, r.getDisplayType());

        r.put("type", "something_else");
        assertFalse(r.isBoundary());
        assertFalse(r.isMultipolygon());
        assertEquals(OsmPrimitiveType.RELATION, r.getDisplayType());
    }
}
