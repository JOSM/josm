// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.DataSource;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.event.DataSourceAddedEvent;
import org.openstreetmap.josm.data.osm.event.DataSourceRemovedEvent;
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
     * Unit test for {@link DataSet#searchPrimitives}
     */
    @Test
    public void testSearchPrimitives() {
        final DataSet ds = new DataSet();
        // null bbox => empty list
        Assert.assertTrue("Empty data set should produce an empty list.", ds.searchPrimitives(null).isEmpty());

        // empty data set, any bbox => empty list
        BBox bbox = new BBox(new LatLon(-180, -90), new LatLon(180, 90));
        Assert.assertTrue("Empty data set should produce an empty list.", ds.searchPrimitives(bbox).isEmpty());
        // data set with elements in the given bbox => these elements
        Node node = new Node(LatLon.ZERO);
        Node node2 = new Node(new LatLon(-0.01, -0.01));
        Way way = TestUtils.newWay("", node, node2);
        Relation r = new Relation(1);
        RelationMember rm = new RelationMember("role", node);
        r.addMember(rm);
        way.getNodes().forEach(ds::addPrimitive);
        ds.addPrimitive(way);
        ds.addPrimitive(r);
        bbox = new BBox(new LatLon(-1.0, -1.0), new LatLon(1.0, 1.0));
        List<OsmPrimitive> result = ds.searchPrimitives(bbox);
        Assert.assertEquals("We should have found four items.", 4, result.size());
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

    /**
     * Unit test for {@link DataSet#DataSet(DataSet)}.
     */
    @Test
    public void testCopyConstructor() {
        DataSet ds = new DataSet();
        assertEqualsDataSet(ds, new DataSet(ds));

        ds.setVersion("fake_version");
        ds.setUploadPolicy(UploadPolicy.BLOCKED);
        Node n1 = new Node(LatLon.SOUTH_POLE);
        Node n2 = new Node(LatLon.NORTH_POLE);
        Way w = new Way(1);
        w.setNodes(Arrays.asList(n1, n2));
        ds.addPrimitive(n1);
        ds.addPrimitive(n2);
        ds.addPrimitive(w);
        Relation r1 = new Relation(1);
        Relation r2 = new Relation(2);
        r2.addMember(new RelationMember("role1", n1));
        r2.addMember(new RelationMember("role2", w));
        r2.addMember(new RelationMember("role3", r1));
        ds.addPrimitive(r1);
        ds.addPrimitive(r2);
        assertEqualsDataSet(ds, new DataSet(ds));
    }

    /**
     * Unit test for {@link DataSet#mergeFrom} - Policies.
     */
    @Test
    public void testMergePolicies() {
        DataSet ds1 = new DataSet();
        DataSet ds2 = new DataSet();

        ds1.setUploadPolicy(UploadPolicy.BLOCKED);
        ds2.setUploadPolicy(UploadPolicy.NORMAL);
        ds1.mergeFrom(ds2);
        assertEquals(UploadPolicy.BLOCKED, ds1.getUploadPolicy());

        ds1.setUploadPolicy(UploadPolicy.NORMAL);
        ds2.setUploadPolicy(UploadPolicy.BLOCKED);
        ds1.mergeFrom(ds2);
        assertEquals(UploadPolicy.BLOCKED, ds1.getUploadPolicy());

        ds1.setDownloadPolicy(DownloadPolicy.BLOCKED);
        ds2.setDownloadPolicy(DownloadPolicy.NORMAL);
        ds1.mergeFrom(ds2);
        assertEquals(DownloadPolicy.BLOCKED, ds1.getDownloadPolicy());

        ds1.setDownloadPolicy(DownloadPolicy.NORMAL);
        ds2.setDownloadPolicy(DownloadPolicy.BLOCKED);
        ds1.mergeFrom(ds2);
        assertEquals(DownloadPolicy.BLOCKED, ds1.getDownloadPolicy());

        ds2.lock();
        assertFalse(ds1.isLocked());
        assertTrue(ds2.isLocked());
        ds1.mergeFrom(ds2);
        assertTrue(ds1.isLocked());
    }

    private static void assertEqualsDataSet(DataSet ds1, DataSet ds2) {
        assertEquals(new ArrayList<>(ds1.getNodes()), new ArrayList<>(ds2.getNodes()));
        assertEquals(new ArrayList<>(ds1.getWays()), new ArrayList<>(ds2.getWays()));
        assertEquals(new ArrayList<>(ds1.getRelations()), new ArrayList<>(ds2.getRelations()));
        assertEquals(new ArrayList<>(ds1.getDataSources()), new ArrayList<>(ds2.getDataSources()));
        assertEquals(ds1.getUploadPolicy(), ds2.getUploadPolicy());
        assertEquals(ds1.getVersion(), ds2.getVersion());
    }

    /**
     * Checks that enum values are defined in the correct order.
     */
    @Test
    public void testEnumOrder() {
        assertTrue(DownloadPolicy.BLOCKED.compareTo(DownloadPolicy.NORMAL) > 0);
        assertTrue(UploadPolicy.BLOCKED.compareTo(UploadPolicy.NORMAL) > 0);
        assertTrue(UploadPolicy.BLOCKED.compareTo(UploadPolicy.DISCOURAGED) > 0);
        assertTrue(UploadPolicy.DISCOURAGED.compareTo(UploadPolicy.NORMAL) > 0);
    }

    /**
     * Checks that data source listeners get called when a data source is added
     */
    @Test
    public void testAddDataSourceListener() {
        DataSourceListener addListener = new DataSourceListener() {
            @Override
            public void dataSourceChange(DataSourceChangeEvent event) {
                assertTrue(event instanceof DataSourceAddedEvent);
            }
        };

        DataSet ds = new DataSet();
        ds.addDataSourceListener(addListener);
        ds.addDataSource(new DataSource(new Bounds(0, 0, 0.1, 0.1), "fake source"));

    }

    /**
     * Checks that data source listeners get called when a data source is removed
     */
    @Test
    public void testRemoveDataSourceListener() {
        DataSourceListener removeListener = new DataSourceListener() {
            @Override
            public void dataSourceChange(DataSourceChangeEvent event) {
                assertTrue(event instanceof DataSourceRemovedEvent);
            }
        };

        DataSet ds = new DataSet();
        ds.addDataSource(new DataSource(new Bounds(0, 0, 0.1, 0.1), "fake source"));
        ds.addDataSourceListener(removeListener);
        new DataSet().mergeFrom(ds);
    }

    /**
     * Checks that a read-only dataset can be cloned.
     */
    @Test
    public void testCloneReadOnly() {
        DataSet ds = new DataSet();
        Node n1 = new Node(LatLon.SOUTH_POLE);
        Node n2 = new Node(LatLon.NORTH_POLE);
        ds.addPrimitive(n1);
        ds.addPrimitive(n2);
        Way w = new Way();
        w.setNodes(Arrays.asList(n1, n2));
        ds.addPrimitive(w);
        Relation r = new Relation();
        r.setMembers(Arrays.asList(new RelationMember(null, w)));
        ds.addPrimitive(r);
        ds.lock();

        DataSet copy = new DataSet(ds);

        assertEquals(4, copy.allPrimitives().size());
        assertTrue(copy.isLocked());
    }
}
