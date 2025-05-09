// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.conflict.ConflictCollection;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.session.SessionReader;
import org.openstreetmap.josm.tools.Logging;

/**
 * Unit tests for class {@link DataSetMerger}.
 */
class DataSetMergerTest {
    private DataSet my;
    private DataSet their;

    /**
     * Setup test.
     */
    @BeforeEach
    public void setUp() {
        my = new DataSet();
        my.setVersion("0.6");
        their = new DataSet();
        their.setVersion("0.6");
        ProjectionRegistry.setProjection(Projections.getProjectionByCode("EPSG:3857")); // Mercator
    }

    private void runConsistencyTests(DataSet ds) {
        StringWriter writer = new StringWriter();
        DatasetConsistencyTest test = new DatasetConsistencyTest(ds, writer);
        test.checkReferrers();
        test.checkCompleteWaysWithIncompleteNodes();
        test.searchNodes();
        test.searchWays();
        test.referredPrimitiveNotInDataset();
        test.checkZeroNodesWays();
        String result = writer.toString();
        if (!result.isEmpty())
            fail(result);
    }

    @AfterEach
    public void checkDatasets() {
        runConsistencyTests(my);
        runConsistencyTests(their);
    }

    /**
     * two identical nodes, even in id and version. No conflict expected.
     * <p>
     * Can happen if data is loaded in two layers and then merged from one layer
     * on the other.
     */
    @Test
    void testNodeSimpleIdenticalNoConflict() {
        Node n = new Node(LatLon.ZERO);
        n.setOsmId(1, 1);
        n.setModified(false);
        n.put("key1", "value1");
        my.addPrimitive(n);

        Node n1 = new Node(LatLon.ZERO);
        n1.setOsmId(1, 1);
        n1.setModified(false);
        n1.put("key1", "value1");
        their.addPrimitive(n1);


        DataSetMerger visitor = new DataSetMerger(my, their);
        visitor.merge();

        Node n2 = (Node) my.getPrimitiveById(1, OsmPrimitiveType.NODE);
        assertTrue(visitor.getConflicts().isEmpty());
        assertNotSame(n1, n2); // make sure we have a clone
        assertEquals(1, n2.getId());
        assertEquals(1, n2.getVersion());
        assertFalse(n2.isModified());
        assertEquals("value1", n2.get("key1"));

        // merge target not modified after merging
        assertFalse(n2.isModified());
    }

    /**
     * two  nodes, my is unmodified, their is updated and has a higher version
     * => their version is going to be the merged version
     */
    @Test
    void testNodeSimpleLocallyUnmodifiedNoConflict() {
        Node n = new Node(LatLon.ZERO);
        n.setOsmId(1, 1);
        n.setModified(false);
        n.put("key1", "value1");
        my.addPrimitive(n);

        Node n1 = new Node(LatLon.ZERO);
        n1.setOsmId(1, 2);
        n1.setModified(false);
        n1.put("key1", "value1-new");
        n1.put("key2", "value2");
        their.addPrimitive(n1);


        DataSetMerger visitor = new DataSetMerger(my, their);
        visitor.merge();

        Node n2 = (Node) my.getPrimitiveById(1, OsmPrimitiveType.NODE);
        assertTrue(visitor.getConflicts().isEmpty());
        assertSame(n, n2); // make sure the merged node is still the original node
        assertSame(n2.getDataSet(), my);
        assertEquals(1, n2.getId());
        assertEquals(2, n2.getVersion());
        assertFalse(n2.isModified());
        assertEquals("value1-new", n2.get("key1"));
        assertEquals("value2", n2.get("key2"));

        // the merge target should not be modified
        assertFalse(n2.isModified());
    }

    /**
     * Node with same id, my is modified, their has a higher version
     * => results in a conflict
     * <p>
     * Use case: node which is modified locally and updated by another mapper on
     * the server
     */
    @Test
    void testNodeSimpleTagConflict() {
        Node n = new Node(LatLon.ZERO);
        n.setOsmId(1, 1);
        n.setModified(true);
        n.put("key1", "value1");
        n.put("key2", "value2");
        my.addPrimitive(n);

        Node n1 = new Node(LatLon.ZERO);
        n1.setOsmId(1, 2);
        n1.setModified(false);
        n1.put("key1", "value1-new");

        their.addPrimitive(n1);


        DataSetMerger visitor = new DataSetMerger(my, their);
        visitor.merge();

        Node n2 = (Node) my.getPrimitiveById(1, OsmPrimitiveType.NODE);
        assertEquals(1, visitor.getConflicts().size());
        assertSame(n, n2);
        assertNotSame(n1, n2);
        assertSame(n1.getDataSet(), their);
    }

    /**
     * node with same id, my is deleted, their has a higher version
     * => results in a conflict
     * <p>
     * Use case: node which is deleted locally and updated by another mapper on
     * the server
     */
    @Test
    void testNodeSimpleDeleteConflict() {
        Node n = new Node(1, 1);
        n.setCoor(LatLon.ZERO);
        n.setDeleted(true);
        n.put("key1", "value1");
        my.addPrimitive(n);

        Node n1 = new Node(LatLon.ZERO);
        n1.setOsmId(1, 2);
        n1.setModified(false);
        n1.put("key1", "value1-new");
        n1.put("key2", "value2");
        their.addPrimitive(n1);


        DataSetMerger visitor = new DataSetMerger(my, their);
        visitor.merge();

        Node n2 = (Node) my.getPrimitiveById(1, OsmPrimitiveType.NODE);
        assertEquals(1, visitor.getConflicts().size());
        assertSame(n, n2);
        assertNotSame(n1, n2);
        assertSame(n1.getDataSet(), their);
    }

    /**
     * My node is deleted, their node has the same id and version and is not deleted.
     * => mine has precedence
     */
    @Test
    void testNodeSimpleDeleteConflict2() {
        Node n = new Node(LatLon.ZERO);
        n.setOsmId(1, 1);
        n.setDeleted(true);
        my.addPrimitive(n);

        Node n1 = new Node(LatLon.ZERO);
        n1.setOsmId(1, 1);
        their.addPrimitive(n1);


        DataSetMerger visitor = new DataSetMerger(my, their);
        visitor.merge();

        Node n2 = (Node) my.getPrimitiveById(1, OsmPrimitiveType.NODE);
        assertEquals(0, visitor.getConflicts().size());
        assertTrue(n2.isVisible());
        assertSame(n, n2);
        assertSame(n.getDataSet(), my);
        assertSame(n1.getDataSet(), their);
    }

    /**
     * My and their node are new but semantically equal. My node is deleted.
     * <p>
     * => Ignore my node, no conflict
     */
    @Test
    void testNodeSimpleDeleteConflict3() {
        Node n = new Node(new LatLon(1, 1));
        n.setDeleted(true);
        my.addPrimitive(n);

        Node n1 = new Node(new LatLon(1, 1));
        their.addPrimitive(n1);


        DataSetMerger visitor = new DataSetMerger(my, their);
        visitor.merge();

        assertEquals(0, visitor.getConflicts().size());
        assertSame(n.getDataSet(), my);
        assertSame(n1.getDataSet(), their);
    }

    /**
     * My and their node are new but semantically equal. Both are deleted.
     * <p>
     * => take mine
     */
    @Test
    void testNodeSimpleDeleteConflict4() {
        Node n = new Node(new LatLon(1, 1));
        n.setDeleted(true);
        my.addPrimitive(n);

        Node n1 = new Node(new LatLon(1, 1));
        n1.setDeleted(true);
        their.addPrimitive(n1);

        DataSetMerger visitor = new DataSetMerger(my, their);
        visitor.merge();

        assertEquals(0, visitor.getConflicts().size());
        Node n2 = (Node) my.getNodes().toArray()[0];
        assertSame(n2, n);
        assertTrue(n2.isDeleted());
    }

    /**
     * their node has no assigned id (id == 0) and is semantically equal to one of my
     * nodes with id == 0
     * <p>
     * => merge it onto my node.
     */
    @Test
    void testNodeSimpleNoIdSemanticallyEqual() {

        User myUser = User.createOsmUser(1111, "my");

        User theirUser = User.createOsmUser(222, "their");

        Node n = new Node();
        n.setCoor(LatLon.ZERO);
        n.put("key1", "value1");
        n.setUser(myUser);
        n.setInstant(Instant.now());

        my.addPrimitive(n);

        Node n1 = new Node();
        n1.setCoor(LatLon.ZERO);
        n1.put("key1", "value1");
        n1.setInstant(Instant.now().plusSeconds(3600));
        n1.setUser(theirUser);
        their.addPrimitive(n1);

        DataSetMerger visitor = new DataSetMerger(my, their);
        visitor.merge();

        Node n2 = my.getNodes().iterator().next();
        assertEquals(0, visitor.getConflicts().size());
        assertEquals("value1", n2.get("key1"));
        assertEquals(n1.getRawTimestamp(), n2.getRawTimestamp());
        assertEquals(theirUser, n2.getUser());
        assertSame(n2, n);
        assertNotSame(n2, n1);
        assertSame(n2.getDataSet(), my);
    }

    /**
     * my node is incomplete, their node is complete
     * <p>
     * => merge it onto my node. My node becomes complete
     */
    @Test
    void testNodeSimpleIncompleteNode() {

        Node n = new Node(1);
        my.addPrimitive(n);

        Node n1 = new Node();
        n1.setCoor(LatLon.ZERO);
        n1.setOsmId(1, 1);
        n1.put("key1", "value1");
        n1.setInstant(Instant.now());
        their.addPrimitive(n1);

        DataSetMerger visitor = new DataSetMerger(my, their);
        visitor.merge();

        Node n2 = my.getNodes().iterator().next();
        assertEquals(0, visitor.getConflicts().size());
        assertEquals("value1", n2.get("key1"));
        assertEquals(n1.getRawTimestamp(), n2.getRawTimestamp());
        assertFalse(n2.isIncomplete());
        assertSame(n2, n);
    }

    /**
     * their way has a higher version and different tags. the nodes are the same. My
     * way is not modified. Merge is possible. No conflict.
     * <p>
     * => merge it onto my way.
     */
    @Test
    void testWaySimpleIdenticalNodesDifferentTags() {

        // -- the target dataset

        Node n1 = new Node();
        n1.setCoor(LatLon.ZERO);
        n1.setOsmId(1, 1);
        my.addPrimitive(n1);

        Node n2 = new Node();
        n2.setCoor(LatLon.ZERO);
        n2.setOsmId(2, 1);

        my.addPrimitive(n2);

        Way myWay = new Way();
        myWay.setOsmId(3, 1);
        myWay.put("key1", "value1");
        myWay.addNode(n1);
        myWay.addNode(n2);
        my.addPrimitive(myWay);

        // -- the source data set

        Node n3 = new Node(LatLon.ZERO);
        n3.setOsmId(1, 1);
        their.addPrimitive(n3);

        Node n4 = new Node(new LatLon(1, 1));
        n4.setOsmId(2, 1);
        their.addPrimitive(n4);

        Way theirWay = new Way();
        theirWay.setOsmId(3, 2);
        theirWay.put("key1", "value1");
        theirWay.put("key2", "value2");
        theirWay.addNode(n3);
        theirWay.addNode(n4);
        their.addPrimitive(theirWay);


        DataSetMerger visitor = new DataSetMerger(my, their);
        visitor.merge();

        // -- tests
        Way merged = (Way) my.getPrimitiveById(3, OsmPrimitiveType.WAY);
        assertEquals(0, visitor.getConflicts().size());
        assertEquals("value1", merged.get("key1"));
        assertEquals("value2", merged.get("key2"));
        assertEquals(3, merged.getId());
        assertEquals(2, merged.getVersion());
        assertEquals(2, merged.getNodesCount());
        assertEquals(1, merged.getNode(0).getId());
        assertEquals(2, merged.getNode(1).getId());
        assertSame(merged, myWay);
        assertSame(merged.getDataSet(), my);

        Node mergedNode = (Node) my.getPrimitiveById(1, OsmPrimitiveType.NODE);
        assertSame(mergedNode, n1);
        mergedNode = (Node) my.getPrimitiveById(2, OsmPrimitiveType.NODE);
        assertSame(mergedNode, n2);

        assertFalse(merged.isModified());
    }

    /**
     * their way has a higher version and different tags. And it has more nodes. Two
     * of the existing nodes are modified.
     * <p>
     * => merge it onto my way, no conflict
     */
    @Test
    void testWaySimpleAdditionalNodesAndChangedNodes() {

        // -- my data set

        Node n1 = new Node(LatLon.ZERO);
        n1.setOsmId(1, 1);
        my.addPrimitive(n1);

        Node n2 = new Node(new LatLon(1, 1));
        n2.setOsmId(2, 1);
        my.addPrimitive(n2);

        Way myWay = new Way();
        myWay.setOsmId(3, 1);
        myWay.addNode(n1);
        myWay.addNode(n2);
        my.addPrimitive(myWay);

        // --- their data set

        Node n3 = new Node(LatLon.ZERO);
        n3.setOsmId(1, 1);
        their.addPrimitive(n3);

        Node n5 = new Node(new LatLon(1, 1));
        n5.setOsmId(4, 1);

        their.addPrimitive(n5);

        Node n4 = new Node(new LatLon(2, 2));
        n4.setOsmId(2, 2);
        n4.put("key1", "value1");
        their.addPrimitive(n4);


        Way theirWay = new Way();
        theirWay.setOsmId(3, 2);
        theirWay.addNode(n3);
        theirWay.addNode(n5); // insert a node
        theirWay.addNode(n4); // this one is updated
        their.addPrimitive(theirWay);

        DataSetMerger visitor = new DataSetMerger(my, their);
        visitor.merge();

        // -- tests
        Way merged = (Way) my.getPrimitiveById(3, OsmPrimitiveType.WAY);
        assertEquals(0, visitor.getConflicts().size());
        assertEquals(3, merged.getId());
        assertEquals(2, merged.getVersion());
        assertEquals(3, merged.getNodesCount());
        assertEquals(1, merged.getNode(0).getId());
        assertEquals(4, merged.getNode(1).getId());
        assertEquals(2, merged.getNode(2).getId());
        assertEquals("value1", merged.getNode(2).get("key1"));

        assertSame(merged.getNode(0), n1);
        assertNotSame(merged.getNode(1), n5); // must be a clone of the original node in their
        assertSame(merged.getNode(2), n2);

        assertFalse(merged.isModified());  // the target wasn't modified before merging, it mustn't be after merging
    }

    /**
     * their way has a higher version and different nodes. My way is modified.
     * <p>
     * => merge onto my way not possible, create a conflict
     */
    @Test
    void testWaySimpleDifferentNodesAndMyIsModified() {

        // -- the target dataset

        Node n1 = new Node(LatLon.ZERO);
        n1.setOsmId(1, 1);
        my.addPrimitive(n1);

        Node n2 = new Node(new LatLon(1, 1));
        n2.setOsmId(2, 1);
        my.addPrimitive(n2);

        Way myWay = new Way();
        myWay.setOsmId(3, 1);

        myWay.addNode(n1);
        myWay.addNode(n2);
        myWay.setModified(true);
        myWay.put("key1", "value1");
        my.addPrimitive(myWay);

        // -- the source dataset

        Node n3 = new Node(LatLon.ZERO);
        n3.setOsmId(1, 1);
        their.addPrimitive(n3);

        Node n5 = new Node(new LatLon(1, 1));
        n5.setOsmId(4, 1);
        their.addPrimitive(n5);

        Node n4 = new Node(new LatLon(2, 2));
        n4.setOsmId(2, 1);
        n4.put("key1", "value1");
        their.addPrimitive(n4);

        Way theirWay = new Way();
        theirWay.setOsmId(3, 2);

        theirWay.addNode(n3);
        theirWay.addNode(n5); // insert a node
        theirWay.addNode(n4); // this one is updated
        their.addPrimitive(theirWay);


        DataSetMerger visitor = new DataSetMerger(my, their);
        visitor.merge();

        Way merged = (Way) my.getPrimitiveById(3, OsmPrimitiveType.WAY);
        assertEquals(1, visitor.getConflicts().size());
        assertEquals(3, merged.getId());
        assertEquals(1, merged.getVersion());
        assertEquals(2, merged.getNodesCount());
        assertEquals(1, merged.getNode(0).getId());
        assertEquals(2, merged.getNode(1).getId());
        assertEquals("value1", merged.get("key1"));
    }

    /**
     * their way is not visible anymore.
     * <p>
     * => conflict
     */
    @Test
    void testWaySimpleTheirVersionNotVisibleMyIsModified() {

        Node mn1 = new Node(LatLon.ZERO);
        mn1.setOsmId(1, 1);
        my.addPrimitive(mn1);

        Node mn2 = new Node(new LatLon(1, 1));
        mn2.setOsmId(2, 1);
        my.addPrimitive(mn2);

        Way myWay = new Way();
        myWay.setOsmId(3, 1);
        myWay.addNode(mn1);
        myWay.addNode(mn2);
        myWay.setModified(true);
        my.addPrimitive(myWay);

        Way theirWay = new Way();
        theirWay.setOsmId(3, 2);
        theirWay.setVisible(false);
        /* Invisible objects fetched from the server should be marked as "deleted".
         * Otherwise it's an error.
         */
        theirWay.setDeleted(true);
        their.addPrimitive(theirWay);

        DataSetMerger visitor = new DataSetMerger(my, their);
        visitor.merge();

        Way merged = (Way) my.getPrimitiveById(3, OsmPrimitiveType.WAY);
        assertEquals(1, visitor.getConflicts().size());
        assertTrue(visitor.getConflicts().hasConflictForMy(myWay));
        assertTrue(visitor.getConflicts().hasConflictForTheir(theirWay));
        assertEquals(myWay, merged);
    }

    /**
     * my and their way have no ids,  nodes they refer to have an id. but
     * my and  their way are semantically equal. so technical attributes of
     * their way can be merged on my way. No conflict.
     */
    @Test
    void testWaySimpleTwoWaysWithNoIdNodesWithId() {

        // -- my data set

        Node n1 = new Node(LatLon.ZERO);
        n1.setOsmId(1, 1);
        my.addPrimitive(n1);

        Node n2 = new Node(new LatLon(1, 1));
        n2.setOsmId(2, 1);
        my.addPrimitive(n2);

        Way myWay = new Way();
        myWay.addNode(n1);
        myWay.addNode(n2);
        my.addPrimitive(myWay);

        // -- their data set

        Node n3 = new Node(LatLon.ZERO);
        n3.setOsmId(1, 1);
        their.addPrimitive(n3);

        Node n4 = new Node(new LatLon(1, 1));
        n4.setOsmId(2, 1);
        their.addPrimitive(n4);

        Way theirWay = new Way();
        theirWay.addNode(n3);
        theirWay.addNode(n4);
        User user = User.createOsmUser(1111, "their");
        theirWay.setUser(user);
        theirWay.setInstant(Instant.now());
        their.addPrimitive(theirWay);

        DataSetMerger visitor = new DataSetMerger(my, their);
        visitor.merge();

        // -- tests
        Way merged = (Way) my.getWays().toArray()[0];
        assertEquals(0, visitor.getConflicts().size());
        assertEquals("their", merged.getUser().getName());
        assertEquals(1111, merged.getUser().getId());
        assertEquals(theirWay.getRawTimestamp(), merged.getRawTimestamp());
        assertSame(merged, myWay);
        assertSame(merged.getNode(0), n1);
        assertSame(merged.getNode(1), n2);

        assertFalse(merged.isModified());
    }

    /**
     * my and their way have no ids, neither do the nodes they refer to. but
     * my and  their way are semantically equal. so technical attributes of
     * their way can be merged on my way. No conflict.
     */
    @Test
    void testWaySimpleTwoWaysWithNoIdNodesWithoutId() {

        // -- my data set

        Node n1 = new Node(LatLon.ZERO);
        my.addPrimitive(n1);

        Node n2 = new Node(new LatLon(1, 1));
        my.addPrimitive(n2);

        Way myWay = new Way();
        myWay.addNode(n1);
        myWay.addNode(n2);
        my.addPrimitive(myWay);

        // -- their data set

        Node n3 = new Node(LatLon.ZERO);
        their.addPrimitive(n3);

        Node n4 = new Node(new LatLon(1, 1));
        their.addPrimitive(n4);

        Way theirWay = new Way();
        theirWay.addNode(n3);
        theirWay.addNode(n4);
        User user = User.createOsmUser(1111, "their");
        theirWay.setUser(user);
        theirWay.setInstant(Instant.now());
        their.addPrimitive(theirWay);

        DataSetMerger visitor = new DataSetMerger(my, their);
        visitor.merge();

        // -- tests
        Way merged = (Way) my.getWays().toArray()[0];
        assertEquals(0, visitor.getConflicts().size());
        assertEquals("their", merged.getUser().getName());
        assertEquals(1111, merged.getUser().getId());
        assertEquals(theirWay.getRawTimestamp(), merged.getRawTimestamp());
        assertSame(merged, myWay);
        assertSame(merged.getNode(0), n1);
        assertSame(merged.getNode(1), n2);

        assertFalse(merged.isModified());
    }

    /**
     * My dataset includes a deleted node.
     * Their dataset includes a way with three nodes, the first one being my node.
     * <p>
     * => the merged way should include all three nodes. Deleted node should have deleted=false and
     * special conflict with isDeleted should exist
     */
    @Test
    void testWayComplexMergingADeletedNode() {

        // -- my dataset

        Node mn1 = new Node(LatLon.ZERO);
        mn1.setOsmId(1, 1);
        mn1.setDeleted(true);
        my.addPrimitive(mn1);

        Node tn1 = new Node(LatLon.ZERO);
        tn1.setOsmId(1, 1);
        their.addPrimitive(tn1);

        Node tn2 = new Node(new LatLon(1, 1));
        tn2.setOsmId(2, 1);
        their.addPrimitive(tn2);

        Node tn3 = new Node(new LatLon(2, 2));
        tn3.setOsmId(3, 1);
        their.addPrimitive(tn3);

        // -- their data set
        Way theirWay = new Way();
        theirWay.setOsmId(4, 1);
        theirWay.addNode(tn1);
        theirWay.addNode(tn2);
        theirWay.addNode(tn3);
        theirWay.setUser(User.createOsmUser(1111, "their"));
        theirWay.setInstant(Instant.now());
        their.addPrimitive(theirWay);

        DataSetMerger visitor = new DataSetMerger(my, their);
        visitor.merge();

        assertEquals(1, visitor.getConflicts().size());
        assertTrue(visitor.getConflicts().get(0).isMyDeleted());

        Way myWay = (Way) my.getPrimitiveById(4, OsmPrimitiveType.WAY);
        assertEquals(3, myWay.getNodesCount());

        Node n = (Node) my.getPrimitiveById(1, OsmPrimitiveType.NODE);
        assertTrue(myWay.getNodes().contains(n));

        assertFalse(myWay.isModified());
    }

    /**
     * My dataset includes a deleted node.
     * Their dataset includes a relation with three nodes, the first one being my node.
     * <p>
     * => the merged relation should include all three nodes. There should be conflict for deleted
     * node with isMyDeleted set
     */
    @Test
    void testRelationComplexMergingADeletedNode() {

        Node mn1 = new Node(LatLon.ZERO);
        mn1.setOsmId(1, 1);
        mn1.setDeleted(true);
        my.addPrimitive(mn1);

        Node tn1 = new Node(LatLon.ZERO);
        tn1.setOsmId(1, 1);
        their.addPrimitive(tn1);

        Node tn2 = new Node(new LatLon(1, 1));
        tn2.setOsmId(2, 1);
        their.addPrimitive(tn2);

        Node tn3 = new Node(new LatLon(2, 2));
        tn3.setOsmId(3, 1);
        their.addPrimitive(tn3);

        Relation theirRelation = new Relation();
        theirRelation.setOsmId(4, 1);

        theirRelation.addMember(new RelationMember("", tn1));
        theirRelation.addMember(new RelationMember("", tn2));
        theirRelation.addMember(new RelationMember("", tn3));
        their.addPrimitive(theirRelation);

        DataSetMerger visitor = new DataSetMerger(my, their);
        visitor.merge();

        Node n = (Node) my.getPrimitiveById(1, OsmPrimitiveType.NODE);
        assertNotNull(n);

        assertEquals(1, visitor.getConflicts().size());
        assertTrue(visitor.getConflicts().hasConflictForMy(n));
        assertTrue(visitor.getConflicts().get(0).isMyDeleted());

        Relation r = (Relation) my.getPrimitiveById(4, OsmPrimitiveType.RELATION);
        assertEquals(3, r.getMembersCount());

        assertFalse(r.isModified());
    }

    /**
     * Merge an incomplete way with two incomplete nodes into an empty dataset.
     * <p>
     * Use case: a way loaded with a multiget, i.e. GET /api/0.6/ways?ids=123456
     */
    @Test
    void testNewIncompleteWay() {

        Node n1 = new Node(1);
        their.addPrimitive(n1);

        Node n2 = new Node(2);
        their.addPrimitive(n2);

        Way w3 = new Way(3);
        w3.setNodes(Arrays.asList(n1, n2));
        their.addPrimitive(w3);
        assertTrue(w3.isIncomplete());

        DataSetMerger visitor = new DataSetMerger(my, their);
        visitor.merge();

        assertEquals(0, visitor.getConflicts().size());

        OsmPrimitive p = my.getPrimitiveById(1, OsmPrimitiveType.NODE);
        assertNotNull(p);
        assertTrue(p.isIncomplete());
        p = my.getPrimitiveById(2, OsmPrimitiveType.NODE);
        assertNotNull(p);
        assertTrue(p.isIncomplete());
        p = my.getPrimitiveById(3, OsmPrimitiveType.WAY);
        assertNotNull(p);
        assertTrue(p.isIncomplete());

        Way w = (Way) my.getPrimitiveById(3, OsmPrimitiveType.WAY);
        assertNotNull(w);
        assertTrue(p.isIncomplete());
        assertEquals(2, w.getNodesCount());
        assertTrue(w.getNode(0).isIncomplete());
        assertTrue(w.getNode(1).isIncomplete());
    }

    /**
     * Merge an incomplete way with two incomplete nodes into a dataset where the way already exists as complete way.
     * <p>
     * Use case: a way loaded with a multiget, i.e. GET /api/0.6/ways?ids=123456 after an "Update selection " of this way
     */
    @Test
    void testIncompleteWayOntoCompleteWay() {

        // an incomplete node
        Node n1 = new Node(1);
        their.addPrimitive(n1);

        // another incomplete node
        Node n2 = new Node(2);
        their.addPrimitive(n2);

        // an incomplete way with two incomplete nodes
        Way w3 = new Way(3);
        w3.setNodes(Arrays.asList(n1, n2));
        their.addPrimitive(w3);

        Node n4 = new Node(LatLon.ZERO);
        n4.setOsmId(1, 1);
        my.addPrimitive(n4);

        Node n5 = new Node(new LatLon(1, 1));
        n5.setOsmId(2, 1);
        my.addPrimitive(n5);

        Way w6 = new Way(3, 1);
        w6.setNodes(Arrays.asList(n4, n5));
        my.addPrimitive(w6);

        DataSetMerger visitor = new DataSetMerger(my, their);
        visitor.merge();

        assertEquals(0, visitor.getConflicts().size());

        OsmPrimitive p = my.getPrimitiveById(1, OsmPrimitiveType.NODE);
        assertNotNull(p);
        assertFalse(p.isIncomplete());
        p = my.getPrimitiveById(2, OsmPrimitiveType.NODE);
        assertNotNull(p);
        assertFalse(p.isIncomplete());
        p = my.getPrimitiveById(3, OsmPrimitiveType.WAY);
        assertNotNull(p);
        assertFalse(p.isIncomplete());

        Way w = (Way) my.getPrimitiveById(3, OsmPrimitiveType.WAY);
        assertNotNull(w);
        assertFalse(p.isIncomplete());
        assertEquals(2, w.getNodesCount());
        assertFalse(w.getNode(0).isIncomplete());
        assertFalse(w.getNode(1).isIncomplete());
    }

    /**
     * merge to complete nodes onto an incomplete way with the same two nodes, but incomplete.
     * => both the nodes and the way should be complete in the target dataset after merging
     */
    @Test
    void testTwoCompleteNodesOntoAnIncompleteWay() {

        // -- source dataset

        // a complete node
        Node n1 = new Node(1, 1);
        n1.setCoor(new LatLon(1, 1));
        their.addPrimitive(n1);

        // another complete node
        Node n2 = new Node(2, 1);
        n2.setCoor(new LatLon(2, 2));
        their.addPrimitive(n2);

        // --- target dataset

        Node n4 = new Node(1);
        my.addPrimitive(n4);

        Node n5 = new Node(2);
        my.addPrimitive(n5);

        Way w6 = new Way(3, 1);
        w6.addNode(n4);
        w6.addNode(n5);
        my.addPrimitive(w6);

        //-- merge it
        DataSetMerger visitor = new DataSetMerger(my, their);
        visitor.merge();

        // -- test it
        assertEquals(0, visitor.getConflicts().size());

        Node n = (Node) my.getPrimitiveById(1, OsmPrimitiveType.NODE);
        assertNotNull(n);
        assertFalse(n.isIncomplete());

        n = (Node) my.getPrimitiveById(2, OsmPrimitiveType.NODE);
        assertNotNull(n);
        assertFalse(n.isIncomplete());

        Way w = (Way) my.getPrimitiveById(3, OsmPrimitiveType.WAY);
        assertNotNull(w);
        assertFalse(w.hasIncompleteNodes());
        assertTrue(w.isUsable());
        assertEquals(2, w.getNodesCount());
        assertEquals(1, w.getNode(0).getId());
        assertEquals(2, w.getNode(1).getId());
    }

    /**
     * merge two equal objects with different visibility,
     * => make sure that DataIntegrityProblemException is thrown.
     */
    @Test
    void testSameVersionAndDifferentVisibility() {

        // -- source dataset

        // a complete node
        Node n1 = new Node(1, 2);
        n1.setCoor(new LatLon(1, 1));
        n1.setVisible(true);
        assertFalse(n1.isModified());
        their.addPrimitive(n1);

        // --- target dataset
        // a complete node
        Node n1b = new Node(1, 2);
        n1b.setCoor(new LatLon(1, 1));
        n1b.setVisible(false);
        assertFalse(n1b.isModified());
        my.addPrimitive(n1b);

        //-- merge it
        DataSetMerger visitor = new DataSetMerger(my, their);
        assertThrows(DataIntegrityProblemException.class, visitor::merge);
    }

    /**
     * node without referrers deleted in source
     */
    @Test
    void testDeletedSingleNode() {

        // -- source dataset

        // a complete node
        Node n1 = new Node(1, 1);
        n1.setCoor(new LatLon(1, 1));
        n1.setVisible(true);
        assertFalse(n1.isModified());
        their.addPrimitive(n1);

        //-- merge it to create identical copies
        DataSetMerger visitor = new DataSetMerger(my, their);
        visitor.merge();

        n1.setDeleted(true);

        visitor = new DataSetMerger(my, their);
        visitor.merge();
        OsmPrimitive n1b = my.getPrimitiveById(n1);
        assertTrue(n1b.isDeleted());
    }

    /**
     * node without referrers deleted in source
     */
    @Test
    void testDeletedSingleNodeWithMonitor() {

        // -- source dataset

        // a complete node
        Node n1 = new Node(1, 1);
        n1.setCoor(new LatLon(1, 1));
        n1.setVisible(true);
        assertFalse(n1.isModified());
        their.addPrimitive(n1);

        //-- merge it to create identical copies
        DataSetMerger visitor = new DataSetMerger(my, their);
        visitor.merge(NullProgressMonitor.INSTANCE);

        n1.setDeleted(true);

        visitor = new DataSetMerger(my, their);
        visitor.merge(NullProgressMonitor.INSTANCE);
        OsmPrimitive n1b = my.getPrimitiveById(n1);
        assertTrue(n1b.isDeleted());
    }

    /**
     * Way without referrers deleted in source
     */
    @Test
    void testDeletedWayNoReferrers() {

        // -- source dataset

        // a complete way with two nodes
        Node n1 = new Node(1, 1);
        n1.setCoor(new LatLon(1, 1));
        Node n2 = new Node(2, 1);
        n2.setCoor(new LatLon(1, 1));
        n1.setVisible(true);
        n2.setVisible(true);
        their.addPrimitive(n1);
        their.addPrimitive(n2);
        Way w1 = new Way(1, 1);
        their.addPrimitive(w1);
        w1.addNode(n1);
        w1.addNode(n2);
        w1.setVisible(true);
        w1.setModified(false);
        assertFalse(n1.isModified());
        assertFalse(n2.isModified());
        assertFalse(w1.isModified());

        //-- merge it to create identical copies
        DataSetMerger visitor = new DataSetMerger(my, their);
        visitor.merge();

        w1.setDeleted(true);

        visitor = new DataSetMerger(my, their);
        visitor.merge();
        OsmPrimitive w1b = my.getPrimitiveById(w1);
        assertTrue(w1b.isDeleted());
    }

    /**
     * Dependency loop in relations, both deleted in source
     * => make sure that DataIntegrityProblemException is thrown.
     */
    @Test
    void testDeletedRelationLoop() {

        // -- source dataset
        Relation r1 = new Relation(1, 1);
        Relation r2 = new Relation(2, 1);
        their.addPrimitive(r1);
        their.addPrimitive(r2);
        // create dependency loop
        r1.addMember(new RelationMember("", r2));
        r2.addMember(new RelationMember("", r1));

        //-- merge it to create identical copies
        DataSetMerger visitor = new DataSetMerger(my, their);
        visitor.merge();

        r1.setMembers(null);
        r2.setMembers(null);
        r1.setDeleted(true);
        r2.setDeleted(true);
        visitor = new DataSetMerger(my, their);
        visitor.merge();

        OsmPrimitive r1b = my.getPrimitiveById(r1);
        OsmPrimitive r2b = my.getPrimitiveById(r2);
        assertTrue(r1b.isDeleted());
        assertTrue(r2b.isDeleted());
    }

    /**
     * Way is deleted in my but member of relation in their
     */
    @Test
    void testDeletedWayStillMemberOfRelation() {

        // -- source dataset
        Node n1 = new Node(1, 1);
        n1.setCoor(new LatLon(1, 1));
        Node n2 = new Node(2, 1);
        n2.setCoor(new LatLon(1, 1));
        n1.setVisible(true);
        n2.setVisible(true);
        their.addPrimitive(n1);
        their.addPrimitive(n2);
        Way w1 = new Way(1, 1);
        their.addPrimitive(w1);
        w1.addNode(n1);
        w1.addNode(n2);
        w1.setVisible(true);
        w1.setModified(false);
        assertFalse(n1.isModified());
        assertFalse(n2.isModified());
        assertFalse(w1.isModified());

        //-- merge it to create identical copies
        DataSetMerger visitor = new DataSetMerger(my, their);
        visitor.merge();

        // let relation use the way that is in my dataset
        Relation r1 = new Relation(1, 1);
        their.addPrimitive(r1);
        r1.addMember(new RelationMember("", w1));

        Way myWay = (Way) my.getPrimitiveById(w1);
        myWay.setNodes(null);
        myWay.setDeleted(true);

        visitor = new DataSetMerger(my, their);
        visitor.merge();
        assertFalse(visitor.getConflicts().isEmpty());
        assertFalse(myWay.isDeleted());
        assertTrue(myWay.isEmpty());
        my.clear(); // prevent error from consistency test
    }

    private void doTestTicket7481(DataSet source, DataSet target) {
        // Local node A
        Node nA = new Node(2848219691L, 1);
        nA.setCoor(LatLon.ZERO);
        nA.setInstant(Instant.parse("2014-05-10T14:25:40Z"));
        nA.setChangesetId(22251108);
        nA.setUser(User.createOsmUser(385987, "yaho"));
        nA.put("name", "Mionga");
        nA.put("tourism", "hotel");

        // Local node B, duplicated
        Node nB = new Node(nA);

        // Move and delete node A
        nA.setCoor(new LatLon(0.1321894855, 6.64627402075));
        nA.setDeleted(true);
        my.addPrimitive(nA);

        // Move and modify node B
        nB.setCoor(new LatLon(0.1322066, 6.6462202));
        nB.put("phone", "999");
        nB.setModified(true);
        their.addPrimitive(nB);

        // Merge
        DataSetMerger visitor = new DataSetMerger(source, target);
        visitor.merge();

        assertEquals(1, visitor.getConflicts().size());
    }

    /**
     * Non-regression test 1 for <a href="https://josm.openstreetmap.de/ticket/7481">Bug #7481</a>.
     */
    @Test
    void testTicket07481ab() {
        doTestTicket7481(my, their);
    }

    /**
     * Non-regression test 2 for <a href="https://josm.openstreetmap.de/ticket/7481">Bug #7481</a>.
     */
    @Test
    void testTicket07481ba() {
        doTestTicket7481(their, my);
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/12599">Bug #12599</a>.
     */
    @Test
    void testTicket12599() {
        // Server node: no modifications
        Node n1 = new Node(1, 1);
        n1.setCoor(LatLon.ZERO);
        assertFalse(n1.isModified());
        their.addPrimitive(n1);

        // Local node: one modification: addition of an uninteresting tag
        Node n1b = new Node(n1);
        n1b.setModified(true);
        n1b.put("note", "something");
        assertTrue(n1b.isModified());
        assertEquals(0, n1b.getInterestingTags().size());
        my.addPrimitive(n1b);

        // Merge
        DataSetMerger visitor = new DataSetMerger(my, their);
        visitor.merge();

        // Check that modification is still here
        Node n = (Node) my.getPrimitiveById(1, OsmPrimitiveType.NODE);
        assertNotNull(n);
        assertEquals("something", n.get("note"));
        assertTrue(n.isModified());

        // Merge again
        visitor = new DataSetMerger(my, their);
        visitor.merge();

        // Check that modification is still here
        n = (Node) my.getPrimitiveById(1, OsmPrimitiveType.NODE);
        assertNotNull(n);
        assertEquals("something", n.get("note"));
        assertTrue(n.isModified());
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/12616">Bug #12616</a>.
     */
    @Test
    void testTicket12616() {
        // Server node: no modifications
        Node n1 = new Node(1, 1);
        n1.setCoor(LatLon.ZERO);
        assertFalse(n1.isModified());
        their.addPrimitive(n1);

        // Local node: one modification: move
        Node n1b = new Node(n1);
        n1b.setCoor(new LatLon(1, 1));
        n1b.setModified(true);
        assertTrue(n1b.isModified());
        assertEquals(new LatLon(1, 1), n1b.getCoor());
        my.addPrimitive(n1b);

        // Merge
        DataSetMerger visitor = new DataSetMerger(my, their);
        visitor.merge();

        // Check that modification is still here
        Node n = (Node) my.getPrimitiveById(1, OsmPrimitiveType.NODE);
        assertNotNull(n);
        assertEquals(new LatLon(1, 1), n.getCoor());
        assertTrue(n.isModified());

        // Merge again
        visitor = new DataSetMerger(my, their);
        visitor.merge();

        // Check that modification is still here
        n = (Node) my.getPrimitiveById(1, OsmPrimitiveType.NODE);
        assertNotNull(n);
        assertEquals(new LatLon(1, 1), n.getCoor());
        assertTrue(n.isModified());
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/19783">Bug #19783</a>.
     */
    @Test
    void testTicket19783() {
        // Server node: deleted
        Node n1 = new Node(1, 2);
        n1.setDeleted(true);
        n1.setVisible(false);
        n1.setModified(false);
        assertFalse(n1.isModified());
        their.addPrimitive(n1);

        // Local node: one modification: move
        Node n1b = new Node(1, 1);
        n1b.setCoor(new LatLon(1, 1));
        n1.setIncomplete(false);
        assertFalse(n1b.isModified());
        my.addPrimitive(n1b);
        n1b.setDeleted(true);
        assertTrue(n1b.isModified());

        // Merge
        DataSetMerger visitor = new DataSetMerger(my, their);
        visitor.merge();

        // Check that modification is gone and version was merged
        Node n = (Node) my.getPrimitiveById(1, OsmPrimitiveType.NODE);
        assertNotNull(n);
        assertFalse(n.isModified());
        assertFalse(n.isVisible());
        assertTrue(n.isDeleted());
        assertEquals(2, n.getVersion());
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/20091">Bug #20091</a>.
     * Relation refers to incomplete way that ways deleted on server.
     */
    @Test
    void testTicket20091() {
        // Server way: deleted
        Way w1 = new Way(1, 2);
        w1.setDeleted(true);
        w1.setVisible(false);
        w1.setModified(false);
        assertFalse(w1.isModified());
        their.addPrimitive(w1);

        Way w1b = new Way(1);
        w1b.setIncomplete(true);
        my.addPrimitive(w1b);
        Relation r1 = new Relation(1, 1);
        r1.addMember(new RelationMember("outer", w1b));
        r1.setModified(true);
        my.addPrimitive(r1);

        // Merge
        DataSetMerger visitor = new DataSetMerger(my, their);
        visitor.merge();

        assertEquals(1, visitor.getConflicts().size());
        assertTrue(r1.isModified());
        assertEquals(w1b, visitor.getConflicts().iterator().next().getMy());
    }

    static Stream<BiConsumer<Node, Node>> testTicket23846() {
        return Stream.of(
                (firstNode, secondNode) -> firstNode.setModified(true),
                (firstNode, secondNode) -> { /* No modifications */ }
        );
    }

    @ParameterizedTest
    @MethodSource
    void testTicket23846(BiConsumer<Node, Node> nodeSetup) {
        final Node firstNode = new Node(1234, 1);
        final Node secondNode = new Node(1234, 1);
        final DataSetMerger merge = new DataSetMerger(my, their);
        firstNode.setCoor(LatLon.ZERO);
        secondNode.setCoor(LatLon.ZERO);
        nodeSetup.accept(firstNode, secondNode);
        my.addPrimitive(firstNode);
        their.addPrimitive(secondNode);
        secondNode.setReferrersDownloaded(true);
        assertFalse(firstNode.isReferrersDownloaded());
        assertTrue(secondNode.isReferrersDownloaded());
        merge.merge();
        assertTrue(firstNode.isReferrersDownloaded());
        assertTrue(secondNode.isReferrersDownloaded());
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/23930">#23930</a>
     */
    @Test
    void testTicket23930() throws IOException, IllegalDataException {
        final File file = new File(TestUtils.getRegressionDataFile(23930, "JOSM_conflict.joz"));
        final SessionReader reader = new SessionReader();
        reader.loadSession(file, true, NullProgressMonitor.INSTANCE);
        final List<OsmDataLayer> layers = reader.getLayers().stream()
                .filter(OsmDataLayer.class::isInstance).map(OsmDataLayer.class::cast).collect(Collectors.toList());
        final DataSet newWay = layers.stream().filter(layer -> layer.getName().equals("new_way.osm"))
                .map(OsmDataLayer::getDataSet).findFirst().orElseThrow();
        final DataSet nodeDeleted = layers.stream().filter(layer -> layer.getName().equals("node_deleted.osm"))
                .map(OsmDataLayer::getDataSet).findFirst().orElseThrow();
        final DataSetMerger merge = new DataSetMerger(nodeDeleted, newWay);
        Logging.clearLastErrorAndWarnings();
        assertDoesNotThrow(() -> merge.merge(NullProgressMonitor.INSTANCE));
        assertTrue(Logging.getLastErrorAndWarnings().isEmpty(), String.join("\n", Logging.getLastErrorAndWarnings()));
        final ConflictCollection conflicts = merge.getConflicts();
        // There are a few differences in the files
        // 1. New node in layer 2: No need for conflict
        // 2. node 2427358529: layer 1 deletes it, layer 2 modifies it (conflict required)
        // 3. new way in layer 2 with new node and node 2427358529 (conflict required)
        // 4. Modification of way 32277602 in layer 1 removing node 2427358529 (conflict required)
        // Therefore, conflicts are as follows:
        // 1. A deleted node (n2427358529) with referrers (w32277602 and new way) and new tags ("fix tag=recheck position")
        assertEquals(1, conflicts.size());
        final Conflict<?> conflict = conflicts.iterator().next();
        final Node myNode = assertInstanceOf(Node.class, conflict.getMy());
        final Node theirNode = assertInstanceOf(Node.class, conflict.getTheir());
        assertFalse(theirNode.isDeleted());
        assertFalse(myNode.isDeleted());
    }
}
