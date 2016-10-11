// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import org.junit.Rule;
import org.junit.Test;
import org.junit.Assert;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.openstreetmap.josm.data.coor.LatLon;

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
        BBox bbox = new BBox(LatLon.NORTH_POLE, LatLon.SOUTH_POLE);
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
}
