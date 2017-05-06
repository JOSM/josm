// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
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
     * Unit test of methods {@link DataSet#addChangeSetTag} / {@link DataSet#getChangeSetTags}.
     */
    @Test
    public void testChangesetTags() {
        final DataSet ds = new DataSet();
        assertTrue(ds.getChangeSetTags().isEmpty());
        ds.addChangeSetTag("foo", "bar");
        assertEquals("bar", ds.getChangeSetTags().get("foo"));
    }

    /**
     * Unit test of methods {@link DataSet#allNonDeletedPrimitives}
     *                    / {@link DataSet#allNonDeletedCompletePrimitives}
     *                    / {@link DataSet#allNonDeletedPhysicalPrimitives}.
     */
    @Test
    public void testAllNonDeleted() {
        final DataSet ds = new DataSet();
        assertTrue(ds.allNonDeletedPrimitives().isEmpty());
        assertTrue(ds.allNonDeletedCompletePrimitives().isEmpty());
        assertTrue(ds.allNonDeletedPhysicalPrimitives().isEmpty());

        Node n1 = new Node(1); n1.setCoor(LatLon.NORTH_POLE); n1.setDeleted(true); n1.setIncomplete(false); ds.addPrimitive(n1);
        Node n2 = new Node(2); n2.setCoor(LatLon.NORTH_POLE); n2.setDeleted(false); n2.setIncomplete(false); ds.addPrimitive(n2);
        Node n3 = new Node(3); n3.setCoor(LatLon.NORTH_POLE); n3.setDeleted(false); n3.setIncomplete(true); ds.addPrimitive(n3);

        Way w1 = new Way(1); w1.setDeleted(true); w1.setIncomplete(false); ds.addPrimitive(w1);
        Way w2 = new Way(2); w2.setDeleted(false); w2.setIncomplete(false); ds.addPrimitive(w2);
        Way w3 = new Way(3); w3.setDeleted(false); w3.setIncomplete(true); ds.addPrimitive(w3);

        Relation r1 = new Relation(1); r1.setDeleted(true); r1.setIncomplete(false); ds.addPrimitive(r1);
        Relation r2 = new Relation(2); r2.setDeleted(false); r2.setIncomplete(false); ds.addPrimitive(r2);
        Relation r3 = new Relation(3); r3.setDeleted(false); r3.setIncomplete(true); ds.addPrimitive(r3);

        assertEquals(new HashSet<>(Arrays.asList(n2, n3, w2, w3, r2, r3)),
                new HashSet<>(ds.allNonDeletedPrimitives()));
        assertEquals(new HashSet<>(Arrays.asList(n2, w2, r2)),
                new HashSet<>(ds.allNonDeletedCompletePrimitives()));
        assertEquals(new HashSet<>(Arrays.asList(n2, w2)),
                new HashSet<>(ds.allNonDeletedPhysicalPrimitives()));
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

    /**
     * Test the selection order.
     * See <a href="https://josm.openstreetmap.de/ticket/14737">#14737</a>
     * @since 12069
     */
    @Test
    public void testSelectionOrderPreserved() {
        final DataSet ds = new DataSet();
        Node n1 = new Node(1);
        Node n2 = new Node(2);
        Node n3 = new Node(3);
        ds.addPrimitive(n1);
        ds.addPrimitive(n2);
        ds.addPrimitive(n3);

        assertEquals(Arrays.asList(), new ArrayList<>(ds.getSelected()));

        ds.setSelected(n1.getPrimitiveId(), n2.getPrimitiveId());
        assertEquals(Arrays.asList(n1, n2), new ArrayList<>(ds.getSelected()));

        ds.clearSelection();
        assertEquals(Arrays.asList(), new ArrayList<>(ds.getSelected()));

        ds.addSelected(n3.getPrimitiveId());
        ds.addSelected(n1.getPrimitiveId(), n2.getPrimitiveId());
        assertEquals(Arrays.asList(n3, n1, n2), new ArrayList<>(ds.getSelected()));

        ds.addSelected(n3.getPrimitiveId());
        assertEquals(Arrays.asList(n3, n1, n2), new ArrayList<>(ds.getSelected()));

        ds.clearSelection(n1.getPrimitiveId());
        assertEquals(Arrays.asList(n3, n2), new ArrayList<>(ds.getSelected()));

        ds.toggleSelected(n1.getPrimitiveId());
        assertEquals(Arrays.asList(n3, n2, n1), new ArrayList<>(ds.getSelected()));

        ds.toggleSelected(n2.getPrimitiveId());
        assertEquals(Arrays.asList(n3, n1), new ArrayList<>(ds.getSelected()));

    }
}
