// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link DataSet}.
 */
public class DataSetTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Unit test of method {@link DataSet#searchRelations}.
     */
    @Test
    public void testSearchRelations() {
        final DataSet ds = new DataSet();
        // null bbox => empty list
        Assert.assertTrue(
            "Empty data set should produce an empty list.",
            ds.searchRelations(null).isEmpty()
        );

        // empty data set, any bbox => empty list
        BBox bbox = new BBox(new LatLon(-180, -90), new LatLon(180, 90));
        Assert.assertTrue(
            "Empty data set should produce an empty list.",
            ds.searchRelations(bbox).isEmpty()
        );

        // data set with elements in the given bbox => these elements
        Node node = new Node(LatLon.ZERO);
        Relation r = new Relation(1);
        RelationMember rm = new RelationMember("role", node);
        r.addMember(rm);
        ds.addPrimitive(node);
        ds.addPrimitive(r);
        bbox = new BBox(new LatLon(-1.0, -1.0), new LatLon(1.0, 1.0));
        List<Relation> result = ds.searchRelations(bbox);
        Assert.assertEquals("We should have found only one item.", 1, result.size());
        Assert.assertTrue("The item found is relation r.", result.contains(r));
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/14186">Bug #14186</a>.
     */
    @Test
    public void testTicket14186() {
        final DataSet ds = new DataSet();
        Node n1 = new Node(1);
        Node n2 = new Node(2);
        Node n3 = new Node(3);
        Way w1 = new Way(1);
        w1.setNodes(Arrays.asList(n1, n2, n3));
        Way w2 = new Way(2);
        w2.setNodes(Arrays.asList(n1, n2, n3));
        ds.addPrimitive(n1);
        ds.addPrimitive(n2);
        ds.addPrimitive(n3);
        ds.addPrimitive(w1);
        ds.addPrimitive(w2);
        ds.unlinkNodeFromWays(n2);
    }
}
