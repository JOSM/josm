// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.Projections;

public class DataSetMergerTest {
    /*private static Logger logger = Logger.getLogger(DataSetMergerTest.class.getName());

    static Properties testProperties;

    @BeforeClass
    static public void init() {

        if(System.getProperty("josm.home") == null){
            testProperties = new Properties();

            // load properties
            //
            try {
                testProperties.load(DataSetMergerTest.class.getResourceAsStream("/test-unit-env.properties"));
            } catch(Exception e){
                logger.log(Level.SEVERE, MessageFormat.format("failed to load property file ''{0}''", "/test-unit-env.properties"));
                fail(MessageFormat.format("failed to load property file ''{0}''", "/test-unit-env.properties"));
            }

            // check josm.home
            //
            String josmHome = testProperties.getProperty("josm.home");
            if (josmHome == null) {
                fail(MessageFormat.format("property ''{0}'' not set in test environment", "josm.home"));
            } else {
                File f = new File(josmHome);
                if (! f.exists() || ! f.canRead()) {
                    fail(MessageFormat.format("property ''{0}'' points to ''{1}'' which is either not existing or not readable", "josm.home", josmHome));
                }
            }
            System.setProperty("josm.home", josmHome);
        }
        Main.pref.init(false);

        // init projection
        Main.proj = new Mercator();
    }*/

    @BeforeClass
    public static void init() {
        Main.initApplicationPreferences();
    }

    private DataSet my;
    private DataSet their;

    @Before
    public void setUp() {
        User.clearUserMap();
        my = new DataSet();
        my.setVersion("0.6");
        their = new DataSet();
        their.setVersion("0.6");
        Main.setProjection(Projections.getProjectionByCode("EPSG:3857")); // Mercator
    }

    private void runConsistencyTests(DataSet ds) throws Exception {
        StringWriter writer = new StringWriter();
        DatasetConsistencyTest test =  new DatasetConsistencyTest(ds, writer);
        test.checkReferrers();
        test.checkCompleteWaysWithIncompleteNodes();
        test.searchNodes();
        test.searchWays();
        test.referredPrimitiveNotInDataset();
        test.checkZeroNodesWays();
        String result = writer.toString();
        if (result.length() > 0)
            throw new RuntimeException(result);
    }

    @After
    public void checkDatasets() throws Exception {
        runConsistencyTests(my);
        runConsistencyTests(their);
    }

    /**
     * two identical nodes, even in id and version. No confict expected.
     *
     * Can happen if data is loaded in two layers and then merged from one layer
     * on the other.
     */
    @Test
    public void nodeSimple_IdenticalNoConflict() {
        Node n = new Node(new LatLon(0,0));
        n.setOsmId(1,1);
        n.setModified(false);
        n.put("key1", "value1");
        my.addPrimitive(n);

        Node n1 = new Node(new LatLon(0,0));
        n1.setOsmId(1,1);
        n1.setModified(false);
        n1.put("key1", "value1");
        their.addPrimitive(n1);


        DataSetMerger visitor = new DataSetMerger(my,their);
        visitor.merge();

        Node n2 = (Node)my.getPrimitiveById(1, OsmPrimitiveType.NODE);
        assertTrue(visitor.getConflicts().isEmpty());
        assertTrue(n1 != n2); // make sure we have a clone
        assertEquals(1, n2.getId());
        assertEquals(1, n2.getVersion());
        assertEquals(false, n2.isModified());
        assertEquals("value1", n2.get("key1"));

        // merge target not modified after merging
        assertTrue(!n2.isModified());
    }

    /**
     * two  nodes, my is unmodified, their is updated and has a higher version
     * => their version is going to be the merged version
     *
     */
    @Test
    public void nodeSimple_locallyUnmodifiedNoConflict() {
        Node n = new Node(new LatLon(0,0));
        n.setOsmId(1,1);
        n.setModified(false);
        n.put("key1", "value1");
        my.addPrimitive(n);

        Node n1 = new Node(new LatLon(0,0));
        n1.setOsmId(1,2);
        n1.setModified(false);
        n1.put("key1", "value1-new");
        n1.put("key2", "value2");
        their.addPrimitive(n1);


        DataSetMerger visitor = new DataSetMerger(my,their);
        visitor.merge();

        Node n2 = (Node)my.getPrimitiveById(1, OsmPrimitiveType.NODE);
        assertTrue(visitor.getConflicts().isEmpty());
        assertTrue(n == n2); // make sure the merged node is still the original node
        assertTrue(n2.getDataSet() == my);
        assertEquals(1, n2.getId());
        assertEquals(2, n2.getVersion());
        assertEquals(false, n2.isModified());
        assertEquals("value1-new", n2.get("key1"));
        assertEquals("value2", n2.get("key2"));

        // the merge target should not be modified
        assertTrue(!n2.isModified());
    }

    /**
     * Node with same id, my is modified, their has a higher version
     * => results in a conflict
     *
     * Use case: node which is modified locally and updated by another mapper on
     * the server
     */
    @Test
    public void nodeSimple_TagConflict() {
        Node n = new Node(new LatLon(0,0));
        n.setOsmId(1,1);
        n.setModified(true);
        n.put("key1", "value1");
        n.put("key2", "value2");
        my.addPrimitive(n);

        Node n1 = new Node(new LatLon(0,0));
        n1.setOsmId(1,2);
        n1.setModified(false);
        n1.put("key1", "value1-new");

        their.addPrimitive(n1);


        DataSetMerger visitor = new DataSetMerger(my,their);
        visitor.merge();

        Node n2 = (Node)my.getPrimitiveById(1, OsmPrimitiveType.NODE);
        assertEquals(1,visitor.getConflicts().size());
        assertTrue(n == n2);
        assertTrue(n1 != n2);
        assertTrue(n1.getDataSet() == their);
    }

    /**
     * node with same id, my is deleted, their has a higher version
     * => results in a conflict
     *
     * Use case: node which is deleted locally and updated by another mapper on
     * the server
     */
    @Test
    public void nodeSimple_DeleteConflict() {
        Node n = new Node(1, 1);
        n.setCoor(new LatLon(0,0));
        n.setDeleted(true);
        n.put("key1", "value1");
        my.addPrimitive(n);

        Node n1 = new Node(new LatLon(0,0));
        n1.setOsmId(1,2);
        n1.setModified(false);
        n1.put("key1", "value1-new");
        n1.put("key2", "value2");
        their.addPrimitive(n1);


        DataSetMerger visitor = new DataSetMerger(my,their);
        visitor.merge();

        Node n2 = (Node)my.getPrimitiveById(1, OsmPrimitiveType.NODE);
        assertEquals(1,visitor.getConflicts().size());
        assertTrue(n == n2);
        assertTrue(n1 != n2);
        assertTrue(n1.getDataSet() == their);
    }

    /**
     * My node is deleted, their node has the same id and version and is not deleted.
     * => mine has precedence
     *
     */
    @Test
    public void nodeSimple_DeleteConflict_2() {
        Node n = new Node(new LatLon(0,0));
        n.setOsmId(1,1);
        n.setDeleted(true);
        my.addPrimitive(n);

        Node n1 = new Node(new LatLon(0,0));
        n1.setOsmId(1,1);
        their.addPrimitive(n1);


        DataSetMerger visitor = new DataSetMerger(my,their);
        visitor.merge();

        Node n2 = (Node)my.getPrimitiveById(1, OsmPrimitiveType.NODE);
        assertEquals(0,visitor.getConflicts().size());
        assertEquals(true, n2.isVisible());
        assertTrue(n == n2);
        assertTrue(n.getDataSet() == my);
        assertTrue(n1.getDataSet() == their);
    }

    /**
     * My and their node are new but semantically equal. My node is deleted.
     *
     * => Ignore my node, no conflict
     */
    @Test
    public void nodeSimple_DeleteConflict_3() {
        Node n = new Node(new LatLon(1,1));
        n.setDeleted(true);
        my.addPrimitive(n);

        Node n1 = new Node(new LatLon(1,1));
        their.addPrimitive(n1);


        DataSetMerger visitor = new DataSetMerger(my,their);
        visitor.merge();

        assertEquals(0, visitor.getConflicts().size());
        assertTrue(n.getDataSet() == my);
        assertTrue(n1.getDataSet() == their);
    }

    /**
     * My and their node are new but semantically equal. Both are deleted.
     *
     * => take mine
     *
     */
    @Test
    public void nodeSimple_DeleteConflict_4() {
        Node n = new Node(new LatLon(1,1));
        n.setDeleted(true);
        my.addPrimitive(n);

        Node n1 = new Node(new LatLon(1,1));
        n1.setDeleted(true);
        their.addPrimitive(n1);


        DataSetMerger visitor = new DataSetMerger(my,their);
        visitor.merge();

        assertEquals(0,visitor.getConflicts().size());
        Node n2 = (Node)my.getNodes().toArray()[0];
        assertTrue(n2 == n);
        assertTrue(n2.isDeleted());
    }

    /**
     * their node has no assigned id (id == 0) and is semantically equal to one of my
     * nodes with id == 0
     *
     * => merge it onto my node.
     *
     */
    @Test
    public void nodeSimple_NoIdSemanticallyEqual() {

        Calendar cal = GregorianCalendar.getInstance();
        User myUser = User.createOsmUser(1111, "my");

        User theirUser = User.createOsmUser(222, "their");

        Node n = new Node();
        n.setCoor(new LatLon(0,0));
        n.put("key1", "value1");
        n.setUser(myUser);
        n.setTimestamp(cal.getTime());

        my.addPrimitive(n);

        Node n1 = new Node();
        n1.setCoor(new LatLon(0,0));
        n1.put("key1", "value1");
        cal.add(Calendar.HOUR, 1);
        Date timestamp = cal.getTime();
        n1.setTimestamp(timestamp);
        n1.setUser(theirUser);
        their.addPrimitive(n1);


        DataSetMerger visitor = new DataSetMerger(my,their);
        visitor.merge();

        Node n2 = my.getNodes().iterator().next();
        assertEquals(0,visitor.getConflicts().size());
        assertEquals("value1",n2.get("key1"));
        assertEquals(true, n1.getTimestamp().equals(n2.getTimestamp()));
        assertEquals(theirUser,n2.getUser());
        assertTrue(n2 == n);
        assertTrue(n2 != n1);
        assertTrue(n2.getDataSet() == my);
    }

    /**
     * my node is incomplete, their node is complete
     *
     * => merge it onto my node. My node becomes complete
     *
     */
    @Test
    public void nodeSimple_IncompleteNode() {

        Node n = new Node(1);
        my.addPrimitive(n);

        Node n1 = new Node();
        n1.setCoor(new LatLon(0,0));
        n1.setOsmId(1,1);
        n1.put("key1", "value1");
        Date timestamp = new Date();
        n1.setTimestamp(timestamp);
        their.addPrimitive(n1);


        DataSetMerger visitor = new DataSetMerger(my,their);
        visitor.merge();

        Node n2 = my.getNodes().iterator().next();
        assertEquals(0,visitor.getConflicts().size());
        assertEquals("value1",n2.get("key1"));
        assertEquals(true, n1.getTimestamp().equals(n2.getTimestamp()));
        assertEquals(false, n2.isIncomplete());
        assertTrue(n2 == n);
    }

    /**
     * their way has a higher version and different tags. the nodes are the same. My
     * way is not modified. Merge is possible. No conflict.
     *
     * => merge it onto my way.
     *
     */
    @Test
    public void waySimple_IdenicalNodesDifferentTags() {

        // -- the target dataset

        Node n1 = new Node();
        n1.setCoor(new LatLon(0,0));
        n1.setOsmId(1,1);
        my.addPrimitive(n1);

        Node n2 = new Node();
        n2.setCoor(new LatLon(0,0));
        n2.setOsmId(2,1);

        my.addPrimitive(n2);

        Way myWay = new Way();
        myWay.setOsmId(3,1);
        myWay.put("key1", "value1");
        myWay.addNode(n1);
        myWay.addNode(n2);
        my.addPrimitive(myWay);

        // -- the source data set

        Node n3 = new Node(new LatLon(0,0));
        n3.setOsmId(1,1);
        their.addPrimitive(n3);

        Node n4 = new Node(new LatLon(1,1));
        n4.setOsmId(2,1);
        their.addPrimitive(n4);

        Way theirWay = new Way();
        theirWay.setOsmId(3,2);
        theirWay.put("key1", "value1");
        theirWay.put("key2", "value2");
        theirWay.addNode(n3);
        theirWay.addNode(n4);
        their.addPrimitive(theirWay);


        DataSetMerger visitor = new DataSetMerger(my,their);
        visitor.merge();

        // -- tests
        Way merged = (Way)my.getPrimitiveById(3, OsmPrimitiveType.WAY);
        assertEquals(0,visitor.getConflicts().size());
        assertEquals("value1",merged.get("key1"));
        assertEquals("value2",merged.get("key2"));
        assertEquals(3,merged.getId());
        assertEquals(2,merged.getVersion());
        assertEquals(2,merged.getNodesCount());
        assertEquals(1,merged.getNode(0).getId());
        assertEquals(2,merged.getNode(1).getId());
        assertTrue(merged == myWay);
        assertTrue(merged.getDataSet() == my);

        Node mergedNode = (Node)my.getPrimitiveById(1, OsmPrimitiveType.NODE);
        assertTrue(mergedNode == n1);
        mergedNode = (Node)my.getPrimitiveById(2, OsmPrimitiveType.NODE);
        assertTrue(mergedNode == n2);

        assertTrue(!merged.isModified());
    }

    /**
     * their way has a higher version and different tags. And it has more nodes. Two
     * of the existing nodes are modified.
     *
     * => merge it onto my way, no conflict
     *
     */
    @Test
    public void waySimple_AdditionalNodesAndChangedNodes() {

        // -- my data set

        Node n1 = new Node(new LatLon(0,0));
        n1.setOsmId(1,1);
        my.addPrimitive(n1);

        Node n2 = new Node(new LatLon(1,1));
        n2.setOsmId(2,1);
        my.addPrimitive(n2);

        Way myWay = new Way();
        myWay.setOsmId(3,1);
        myWay.addNode(n1);
        myWay.addNode(n2);
        my.addPrimitive(myWay);

        // --- their data set

        Node n3 = new Node(new LatLon(0,0));
        n3.setOsmId(1,1);
        their.addPrimitive(n3);

        Node n5 = new Node(new LatLon(1,1));
        n5.setOsmId(4,1);

        their.addPrimitive(n5);

        Node n4 = new Node(new LatLon(2,2));
        n4.setOsmId(2,2);
        n4.put("key1", "value1");
        their.addPrimitive(n4);


        Way theirWay = new Way();
        theirWay.setOsmId(3,2);
        theirWay.addNode(n3);
        theirWay.addNode(n5); // insert a node
        theirWay.addNode(n4); // this one is updated
        their.addPrimitive(theirWay);

        DataSetMerger visitor = new DataSetMerger(my,their);
        visitor.merge();

        // -- tests
        Way merged = (Way)my.getPrimitiveById(3, OsmPrimitiveType.WAY);
        assertEquals(0,visitor.getConflicts().size());
        assertEquals(3,merged.getId());
        assertEquals(2,merged.getVersion());
        assertEquals(3,merged.getNodesCount());
        assertEquals(1,merged.getNode(0).getId());
        assertEquals(4,merged.getNode(1).getId());
        assertEquals(2,merged.getNode(2).getId());
        assertEquals("value1",merged.getNode(2).get("key1"));

        assertTrue(merged.getNode(0) == n1);
        assertTrue(merged.getNode(1) != n5); // must be clone of the original node in their
        assertTrue(merged.getNode(2) == n2);

        assertTrue(!merged.isModified());  // the target wasn't modified before merging, it
        // mustn't be after merging
    }

    /**
     * their way has a higher version and different nodes. My way is modified.
     *
     * => merge onto my way not possible, create a conflict
     *
     */
    @Test
    public void waySimple_DifferentNodesAndMyIsModified() {

        // -- the target dataset

        Node n1 = new Node(new LatLon(0,0));
        n1.setOsmId(1,1);
        my.addPrimitive(n1);

        Node n2 = new Node(new LatLon(1,1));
        n2.setOsmId(2,1);
        my.addPrimitive(n2);

        Way myWay = new Way();
        myWay.setOsmId(3,1);

        myWay.addNode(n1);
        myWay.addNode(n2);
        myWay.setModified(true);
        myWay.put("key1", "value1");
        my.addPrimitive(myWay);

        // -- the source dataset

        Node n3 = new Node(new LatLon(0,0));
        n3.setOsmId(1,1);
        their.addPrimitive(n3);

        Node n5 = new Node(new LatLon(1,1));
        n5.setOsmId(4,1);
        their.addPrimitive(n5);

        Node n4 = new Node(new LatLon(2,2));
        n4.setOsmId(2,1);
        n4.put("key1", "value1");
        their.addPrimitive(n4);


        Way theirWay = new Way();
        theirWay.setOsmId(3,2);

        theirWay.addNode(n3);
        theirWay.addNode(n5); // insert a node
        theirWay.addNode(n4); // this one is updated
        their.addPrimitive(theirWay);


        DataSetMerger visitor = new DataSetMerger(my, their);
        visitor.merge();

        Way merged = (Way)my.getPrimitiveById(3, OsmPrimitiveType.WAY);
        assertEquals(1,visitor.getConflicts().size());
        assertEquals(3,merged.getId());
        assertEquals(1,merged.getVersion());
        assertEquals(2,merged.getNodesCount());
        assertEquals(1,merged.getNode(0).getId());
        assertEquals(2,merged.getNode(1).getId());
        assertEquals("value1",merged.get("key1"));
    }


    /**
     * their way is not visible anymore.
     *
     * => conflict
     *
     */
    @Test
    public void waySimple_TheirVersionNotVisibleMyIsModified() {


        Node mn1 = new Node(new LatLon(0,0));
        mn1.setOsmId(1,1);
        my.addPrimitive(mn1);

        Node mn2 = new Node(new LatLon(1,1));
        mn2.setOsmId(2,1);
        my.addPrimitive(mn2);

        Way myWay = new Way();
        myWay.setOsmId(3,1);
        myWay.addNode(mn1);
        myWay.addNode(mn2);
        myWay.setModified(true);
        my.addPrimitive(myWay);


        Way theirWay = new Way();
        theirWay.setOsmId(3,2);
        theirWay.setVisible(false);
        /* Invisible objects fetched from the server should be marked as "deleted".
         * Otherwise it's an error.
         */
        theirWay.setDeleted(true);
        their.addPrimitive(theirWay);

        DataSetMerger visitor = new DataSetMerger(my,their);
        visitor.merge();

        Way merged = (Way)my.getPrimitiveById(3, OsmPrimitiveType.WAY);
        assertEquals(1,visitor.getConflicts().size());
        assertEquals(true, visitor.getConflicts().hasConflictForMy(myWay));
        assertEquals(true, visitor.getConflicts().hasConflictForTheir(theirWay));
        assertEquals(myWay,merged);
    }

    /**
     * my and their way have no ids,  nodes they refer to have an id. but
     * my and  their way are semantically equal. so technical attributes of
     * their way can be merged on my way. No conflict.
     *
     */
    @Test
    public void waySimple_twoWaysWithNoId_NodesWithId() {

        // -- my data set

        Node n1 = new Node(new LatLon(0,0));
        n1.setOsmId(1,1);
        my.addPrimitive(n1);

        Node n2 = new Node(new LatLon(1,1));
        n2.setOsmId(2,1);
        my.addPrimitive(n2);

        Way myWay = new Way();
        myWay.addNode(n1);
        myWay.addNode(n2);
        my.addPrimitive(myWay);

        // -- their data set

        Node n3 = new Node(new LatLon(0,0));
        n3.setOsmId(1,1);
        their.addPrimitive(n3);

        Node n4 = new Node(new LatLon(1,1));
        n4.setOsmId(2,1);
        their.addPrimitive(n4);

        Way theirWay = new Way();
        theirWay.addNode(n3);
        theirWay.addNode(n4);
        User user = User.createOsmUser(1111, "their");
        theirWay.setUser(user);
        theirWay.setTimestamp(new Date());
        their.addPrimitive(theirWay);

        DataSetMerger visitor = new DataSetMerger(my,their);
        visitor.merge();

        // -- tests
        Way merged = (Way)my.getWays().toArray()[0];
        assertEquals(0,visitor.getConflicts().size());
        assertEquals("their", merged.getUser().getName());
        assertEquals(1111, merged.getUser().getId());
        assertEquals(theirWay.getTimestamp(), merged.getTimestamp());
        assertTrue(merged == myWay);
        assertTrue(merged.getNode(0) == n1);
        assertTrue(merged.getNode(1) == n2);

        assertTrue(!merged.isModified());
    }

    /**
     * my and their way have no ids, neither do the nodes they refer to. but
     * my and  their way are semantically equal. so technical attributes of
     * their way can be merged on my way. No conflict.
     *
     */
    @Test
    public void waySimple_twoWaysWithNoId_NodesWithoutId() {

        // -- my data set

        Node n1 = new Node(new LatLon(0,0));
        my.addPrimitive(n1);

        Node n2 = new Node(new LatLon(1,1));
        my.addPrimitive(n2);

        Way myWay = new Way();
        myWay.addNode(n1);
        myWay.addNode(n2);
        my.addPrimitive(myWay);

        // -- their data set

        Node n3 = new Node(new LatLon(0,0));
        their.addPrimitive(n3);

        Node n4 = new Node(new LatLon(1,1));
        their.addPrimitive(n4);

        Way theirWay = new Way();
        theirWay.addNode(n3);
        theirWay.addNode(n4);
        User user = User.createOsmUser(1111, "their");
        theirWay.setUser(user);
        theirWay.setTimestamp(new Date());
        their.addPrimitive(theirWay);

        DataSetMerger visitor = new DataSetMerger(my,their);
        visitor.merge();

        // -- tests
        Way merged = (Way)my.getWays().toArray()[0];
        assertEquals(0,visitor.getConflicts().size());
        assertEquals("their", merged.getUser().getName());
        assertEquals(1111, merged.getUser().getId());
        assertEquals(theirWay.getTimestamp(), merged.getTimestamp());
        assertTrue(merged == myWay);
        assertTrue(merged.getNode(0) == n1);
        assertTrue(merged.getNode(1) == n2);

        assertTrue(!merged.isModified());
    }


    /**
     * My dataset includes a deleted node.
     * Their dataset includes a way with three nodes, the first one being my node.
     *
     * => the merged way should include all three nodes. Deleted node should have deleted=false and
     * special conflict with isDeleted should exist
     *
     */
    @Test
    public void wayComplex_mergingADeletedNode() {

        // -- my dataset

        Node mn1 = new Node(new LatLon(0,0));
        mn1.setOsmId(1,1);
        mn1.setDeleted(true);
        my.addPrimitive(mn1);


        Node tn1 = new Node(new LatLon(0,0));
        tn1.setOsmId(1,1);
        their.addPrimitive(tn1);

        Node tn2 = new Node(new LatLon(1,1));
        tn2.setOsmId(2,1);
        their.addPrimitive(tn2);

        Node tn3 = new Node(new LatLon(2,2));
        tn3.setOsmId(3,1);
        their.addPrimitive(tn3);

        // -- their data set
        Way theirWay = new Way();
        theirWay.setOsmId(4,1);
        theirWay.addNode(tn1);
        theirWay.addNode(tn2);
        theirWay.addNode(tn3);
        theirWay.setUser(User.createOsmUser(1111, "their"));
        theirWay.setTimestamp(new Date());
        their.addPrimitive(theirWay);

        DataSetMerger visitor = new DataSetMerger(my,their);
        visitor.merge();

        assertEquals(1, visitor.getConflicts().size());
        assertTrue(visitor.getConflicts().get(0).isMyDeleted());

        Way myWay = (Way)my.getPrimitiveById(4, OsmPrimitiveType.WAY);
        assertEquals(3, myWay.getNodesCount());

        Node n = (Node)my.getPrimitiveById(1,OsmPrimitiveType.NODE);
        assertTrue(myWay.getNodes().contains(n));

        assertFalse(myWay.isModified());
    }

    /**
     * My dataset includes a deleted node.
     * Their dataset includes a relation with three nodes, the first one being my node.
     *
     * => the merged relation should include all three nodes. There should be conflict for deleted
     * node with isMyDeleted set
     *
     */
    @Test
    public void relationComplex_mergingADeletedNode() {


        Node mn1 = new Node(new LatLon(0,0));
        mn1.setOsmId(1,1);
        mn1.setDeleted(true);
        my.addPrimitive(mn1);


        Node tn1 = new Node(new LatLon(0,0));
        tn1.setOsmId(1,1);
        their.addPrimitive(tn1);

        Node tn2 = new Node(new LatLon(1,1));
        tn2.setOsmId(2,1);
        their.addPrimitive(tn2);

        Node tn3 = new Node(new LatLon(2,2));
        tn3.setOsmId(3,1);
        their.addPrimitive(tn3);


        Relation theirRelation = new Relation();
        theirRelation.setOsmId(4,1);

        theirRelation.addMember(new RelationMember("", tn1));
        theirRelation.addMember(new RelationMember("", tn2));
        theirRelation.addMember(new RelationMember("", tn3));
        their.addPrimitive(theirRelation);

        DataSetMerger visitor = new DataSetMerger(my,their);
        visitor.merge();

        Node n = (Node)my.getPrimitiveById(1,OsmPrimitiveType.NODE);
        assertTrue(n != null);

        assertEquals(1, visitor.getConflicts().size());
        assertTrue(visitor.getConflicts().hasConflictForMy(n));
        assertTrue(visitor.getConflicts().get(0).isMyDeleted());

        Relation r = (Relation)my.getPrimitiveById(4,OsmPrimitiveType.RELATION);
        assertEquals(3, r.getMembersCount());

        assertFalse(r.isModified());
    }

    /**
     * Merge an incomplete way with two incomplete nodes into an empty dataset.
     *
     * Use case: a way loaded with a multiget, i.e. GET /api/0.6/ways?ids=123456
     */
    @Test
    public void newIncompleteWay() {

        Node n1 = new Node(1);
        their.addPrimitive(n1);

        Node n2 = new Node(2);
        their.addPrimitive(n2);

        Way w3 = new Way(3);
        w3.setNodes(Arrays.asList(n1,n2));
        their.addPrimitive(w3);
        assertTrue(w3.isIncomplete());


        DataSetMerger visitor = new DataSetMerger(my, their);
        visitor.merge();

        assertEquals(0,visitor.getConflicts().size());

        OsmPrimitive p= my.getPrimitiveById(1, OsmPrimitiveType.NODE);
        assertNotNull(p);
        assertTrue(p.isIncomplete());
        p= my.getPrimitiveById(2, OsmPrimitiveType.NODE);
        assertNotNull(p);
        assertTrue(p.isIncomplete());
        p= my.getPrimitiveById(3, OsmPrimitiveType.WAY);
        assertNotNull(p);
        assertTrue(p.isIncomplete());

        Way w = (Way)my.getPrimitiveById(3, OsmPrimitiveType.WAY);
        assertNotNull(w);
        assertTrue(p.isIncomplete());
        assertEquals(2, w.getNodesCount());
        assertTrue(w.getNode(0).isIncomplete());
        assertTrue(w.getNode(1).isIncomplete());
    }

    /**
     * Merge an incomplete way with two incomplete nodes into a dataset where the way already exists as complete way.
     *
     * Use case: a way loaded with a multiget, i.e. GET /api/0.6/ways?ids=123456 after a "Update selection " of this way
     */
    @Test
    public void incompleteWayOntoCompleteWay() {

        // an incomplete node
        Node n1 = new Node(1);
        their.addPrimitive(n1);

        // another incomplete node
        Node n2 = new Node(2);
        their.addPrimitive(n2);

        // an incomplete way with two incomplete nodes
        Way w3 = new Way(3);
        w3.setNodes(Arrays.asList(n1,n2));
        their.addPrimitive(w3);


        Node n4 = new Node(new LatLon(0,0));
        n4.setOsmId(1,1);
        my.addPrimitive(n4);

        Node n5 = new Node(new LatLon(1,1));
        n5.setOsmId(2,1);
        my.addPrimitive(n5);

        Way w6 = new Way(3, 1);
        w6.setNodes(Arrays.asList(n4,n5));
        my.addPrimitive(w6);

        DataSetMerger visitor = new DataSetMerger(my,their);
        visitor.merge();

        assertEquals(0,visitor.getConflicts().size());

        OsmPrimitive p= my.getPrimitiveById(1, OsmPrimitiveType.NODE);
        assertNotNull(p);
        assertTrue(!p.isIncomplete());
        p= my.getPrimitiveById(2, OsmPrimitiveType.NODE);
        assertNotNull(p);
        assertTrue(!p.isIncomplete());
        p= my.getPrimitiveById(3,OsmPrimitiveType.WAY);
        assertNotNull(p);
        assertTrue(!p.isIncomplete());

        Way w = (Way)my.getPrimitiveById(3,OsmPrimitiveType.WAY);
        assertNotNull(w);
        assertTrue(!p.isIncomplete());
        assertEquals(2, w.getNodesCount());
        assertTrue(!w.getNode(0).isIncomplete());
        assertTrue(!w.getNode(1).isIncomplete());
    }

    /**
     * merge to complete nodes onto an incomplete way with the same two nodes, but incomplete.
     * => both the nodes and the way should be complete in the target dataset after merging
     */
    @Test
    public void twoCompleteNodesOntoAnIncompleteWay() {

        // -- source dataset

        // an complete node
        Node n1 = new Node(1, 1);
        n1.setCoor(new LatLon(1,1));
        their.addPrimitive(n1);

        // another complete node
        Node n2 = new Node(2, 1);
        n2.setCoor(new LatLon(2,2));
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
        assertEquals(0,visitor.getConflicts().size());

        Node n = (Node)my.getPrimitiveById(1, OsmPrimitiveType.NODE);
        assertNotNull(n);
        assertFalse(n.isIncomplete());

        n = (Node)my.getPrimitiveById(2, OsmPrimitiveType.NODE);
        assertNotNull(n);
        assertFalse(n.isIncomplete());

        Way w = (Way)my.getPrimitiveById(3, OsmPrimitiveType.WAY);
        assertNotNull(w);
        assertFalse(w.hasIncompleteNodes());
        assertTrue(w.isUsable());
        assertEquals(2, w.getNodesCount());
        assertEquals(1, w.getNode(0).getId());
        assertEquals(2, w.getNode(1).getId());
    }
}
