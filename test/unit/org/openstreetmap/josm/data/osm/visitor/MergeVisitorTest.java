// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.projection.Mercator;

public class MergeVisitorTest {
    private static Logger logger = Logger.getLogger(MergeVisitorTest.class.getName());

    static Properties testProperties;

    @BeforeClass
    static public void init() {

        if(System.getProperty("josm.home") == null){
            testProperties = new Properties();

            // load properties
            //
            try {
                testProperties.load(MergeVisitorTest.class.getResourceAsStream("/test-unit-env.properties"));
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
    }

    /**
     * two identical nodes, even in id and version. No confict expected.
     *
     * Can happen if data is loaded in two layers and then merged from one layer
     * on the other.
     */
    @Test
    public void nodeSimple_IdenticalNoConflict() {
        DataSet my = new DataSet();
        my.version = "0.6";
        Node n = new Node(new LatLon(0,0));
        n.setOsmId(1,1);
        n.setModified(false);
        n.put("key1", "value1");
        my.addPrimitive(n);

        DataSet their = new DataSet();
        their.version = "0.6";
        Node n1 = new Node(new LatLon(0,0));
        n1.setOsmId(1,1);
        n1.setModified(false);
        n1.put("key1", "value1");
        their.addPrimitive(n1);


        MergeVisitor visitor = new MergeVisitor(my,their);
        visitor.merge();

        Node n2 = (Node)my.getPrimitiveById(1, OsmPrimitiveType.NODE);
        assertTrue(visitor.getConflicts().isEmpty());
        assertEquals(1, n2.getId());
        assertEquals(1, n2.getVersion());
        assertEquals(false, n2.isModified());
        assertEquals("value1", n2.get("key1"));
    }

    /**
     * two  nodes, my is unmodified, their is updated and has a higher version
     * => their version is going to be the merged version
     *
     */
    @Test
    public void nodeSimple_locallyUnmodifiedNoConflict() {
        DataSet my = new DataSet();
        my.version = "0.6";
        Node n = new Node(new LatLon(0,0));
        n.setOsmId(1,1);
        n.setModified(false);
        n.put("key1", "value1");
        my.addPrimitive(n);

        DataSet their = new DataSet();
        their.version = "0.6";
        Node n1 = new Node(new LatLon(0,0));
        n1.setOsmId(1,2);
        n1.setModified(false);
        n1.put("key1", "value1-new");
        n1.put("key2", "value2");
        their.addPrimitive(n1);


        MergeVisitor visitor = new MergeVisitor(my,their);
        visitor.merge();

        Node n2 = (Node)my.getPrimitiveById(1, OsmPrimitiveType.NODE);
        assertTrue(visitor.getConflicts().isEmpty());
        assertEquals(1, n2.getId());
        assertEquals(2, n2.getVersion());
        assertEquals(false, n2.isModified());
        assertEquals("value1-new", n2.get("key1"));
        assertEquals("value2", n2.get("key2"));
    }

    /**
     * node with same id, my is modified, their has a higher version
     * => results in a conflict
     *
     * Use case: node which is modified locally and updated by another mapper on
     * the server
     */
    @Test
    public void nodeSimple_TagConflict() {
        DataSet my = new DataSet();
        my.version = "0.6";
        Node n = new Node(new LatLon(0,0));
        n.setOsmId(1,1);
        n.setModified(true);
        n.put("key1", "value1");
        n.put("key2", "value2");
        my.addPrimitive(n);

        DataSet their = new DataSet();
        their.version = "0.6";
        Node n1 = new Node(new LatLon(0,0));
        n1.setOsmId(1,2);
        n1.setModified(false);
        n1.put("key1", "value1-new");

        their.addPrimitive(n1);


        MergeVisitor visitor = new MergeVisitor(my,their);
        visitor.merge();

        Node n2 = (Node)my.getPrimitiveById(1, OsmPrimitiveType.NODE);
        assertEquals(1,visitor.getConflicts().size());
        assertEquals(n, n2);
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
        DataSet my = new DataSet();
        my.version = "0.6";
        Node n = new Node(1);
        n.setCoor(new LatLon(0,0));
        n.incomplete = false;
        n.setDeleted(true);
        n.put("key1", "value1");
        my.addPrimitive(n);

        DataSet their = new DataSet();
        their.version = "0.6";
        Node n1 = new Node(new LatLon(0,0));
        n1.setOsmId(1,1);
        n1.setModified(false);
        n1.put("key1", "value1-new");
        n1.put("key2", "value2");
        their.addPrimitive(n1);


        MergeVisitor visitor = new MergeVisitor(my,their);
        visitor.merge();

        Node n2 = (Node)my.getPrimitiveById(1, OsmPrimitiveType.NODE);
        assertEquals(1,visitor.getConflicts().size());
        assertEquals(n, n2);
    }

    /**
     * My node is visible, their version has a higher version and is not visible
     * => create a conflict
     *
     */
    @Test
    public void nodeSimple_VisibleConflict() {
        DataSet my = new DataSet();
        my.version = "0.6";
        Node n = new Node(new LatLon(0,0));
        n.setOsmId(1,1);
        n.setModified(false);
        n.setVisible(true);
        my.addPrimitive(n);

        DataSet their = new DataSet();
        their.version = "0.6";
        Node n1 = new Node(new LatLon(0,0));
        n1.setOsmId(1,2);

        n1.setModified(false);
        n1.setVisible(false);
        their.addPrimitive(n1);


        MergeVisitor visitor = new MergeVisitor(my,their);
        visitor.merge();

        Node n2 = (Node)my.getPrimitiveById(1,OsmPrimitiveType.NODE);
        assertEquals(1,visitor.getConflicts().size());
        assertEquals(true, n2.isVisible());
    }

    /**
     * My node is deleted, their node has the same id and version and is not deleted.
     * => mine has precedence
     *
     */
    @Test
    public void nodeSimple_DeleteConflict_2() {
        DataSet my = new DataSet();
        my.version = "0.6";
        Node n = new Node(new LatLon(0,0));
        n.setOsmId(1,1);
        n.setDeleted(true);
        my.addPrimitive(n);

        DataSet their = new DataSet();
        their.version = "0.6";
        Node n1 = new Node(new LatLon(0,0));
        n1.setOsmId(1,1);
        their.addPrimitive(n1);


        MergeVisitor visitor = new MergeVisitor(my,their);
        visitor.merge();

        Node n2 = (Node)my.getPrimitiveById(1, OsmPrimitiveType.NODE);
        assertEquals(0,visitor.getConflicts().size());
        assertEquals(true, n2.isVisible());
    }

    /**
     * My and their node are new but semantically equal. My node is deleted.
     *
     * => create a conflict
     *
     */
    @Test
    public void nodeSimple_DeleteConflict_3() {
        DataSet my = new DataSet();
        my.version = "0.6";
        Node n = new Node(new LatLon(1,1));
        n.setDeleted(true);
        my.addPrimitive(n);

        DataSet their = new DataSet();
        their.version = "0.6";
        Node n1 = new Node(new LatLon(1,1));
        their.addPrimitive(n1);


        MergeVisitor visitor = new MergeVisitor(my,their);
        visitor.merge();

        assertEquals(1,visitor.getConflicts().size());
    }

    /**
     * My and their node are new but semantically equal. Both are deleted.
     *
     * => take mine
     *
     */
    @Test
    public void nodeSimple_DeleteConflict_4() {
        DataSet my = new DataSet();
        my.version = "0.6";
        Node n = new Node(new LatLon(1,1));
        n.setDeleted(true);
        my.addPrimitive(n);

        DataSet their = new DataSet();
        their.version = "0.6";
        Node n1 = new Node(new LatLon(1,1));
        n1.setDeleted(true);
        their.addPrimitive(n1);


        MergeVisitor visitor = new MergeVisitor(my,their);
        visitor.merge();

        assertEquals(0,visitor.getConflicts().size());
    }

    /**
     * their node is not visible and doesn't exist in my data set
     * => ignore their node
     *
     */
    @Test
    public void nodeSimple_InvisibleNodeInTheirDataset() {
        DataSet my = new DataSet();
        my.version = "0.6";
        Node n = new Node(new LatLon(0,0));
        n.setOsmId(1,1);
        n.setDeleted(true);
        my.addPrimitive(n);

        DataSet their = new DataSet();
        their.version = "0.6";
        Node n1 = new Node(new LatLon(0,0));
        n1.setOsmId(2,1);
        n1.setVisible(false);
        their.addPrimitive(n1);


        MergeVisitor visitor = new MergeVisitor(my,their);
        visitor.merge();

        Node n2 = (Node)my.getPrimitiveById(1,OsmPrimitiveType.NODE);
        assertEquals(0,visitor.getConflicts().size());
        assertEquals(2,my.nodes.size());
        assertEquals(n,n2);
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

        DataSet my = new DataSet();
        my.version = "0.6";
        Node n = new Node();
        n.setCoor(new LatLon(0,0));
        n.put("key1", "value1");
        n.setUser(myUser);
        n.setTimestamp(cal.getTime());

        my.addPrimitive(n);

        DataSet their = new DataSet();
        their.version = "0.6";
        Node n1 = new Node();
        n1.setCoor(new LatLon(0,0));
        n1.put("key1", "value1");
        cal.add(Calendar.HOUR, 1);
        Date timestamp = cal.getTime();
        n1.setTimestamp(timestamp);
        n1.setUser(theirUser);
        their.addPrimitive(n1);


        MergeVisitor visitor = new MergeVisitor(my,their);
        visitor.merge();

        Node n2 = my.nodes.iterator().next();
        assertEquals(0,visitor.getConflicts().size());
        assertEquals("value1",n2.get("key1"));
        assertEquals(true, n1.getTimestamp().equals(n2.getTimestamp()));
        assertEquals(theirUser,n2.getUser());
    }

    /**
     * my node is incomplete, their node is complete
     *
     * => merge it onto my node. My node becomes complete
     *
     */
    @Test
    public void nodeSimple_IncompleteNode() {

        DataSet my = new DataSet();
        my.version = "0.6";
        Node n = new Node();
        n.setCoor(new LatLon(0,0));
        n.setOsmId(1,1);
        n.incomplete = true;
        my.addPrimitive(n);

        DataSet their = new DataSet();
        their.version = "0.6";
        Node n1 = new Node();
        n1.setCoor(new LatLon(0,0));
        n1.setOsmId(1,1);
        n1.put("key1", "value1");
        Date timestamp = new Date();
        n1.setTimestamp(timestamp);
        their.addPrimitive(n1);


        MergeVisitor visitor = new MergeVisitor(my,their);
        visitor.merge();

        Node n2 = my.nodes.iterator().next();
        assertEquals(0,visitor.getConflicts().size());
        assertEquals("value1",n2.get("key1"));
        assertEquals(true, n1.getTimestamp().equals(n2.getTimestamp()));
        assertEquals(false, n2.incomplete);
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


        DataSet my = new DataSet();
        my.version = "0.6";

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

        DataSet their = new DataSet();
        their.version = "0.6";

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


        MergeVisitor visitor = new MergeVisitor(my,their);
        visitor.merge();

        Way merged = (Way)my.getPrimitiveById(3, OsmPrimitiveType.WAY);
        assertEquals(0,visitor.getConflicts().size());
        assertEquals("value1",merged.get("key1"));
        assertEquals("value2",merged.get("key2"));
        assertEquals(3,merged.getId());
        assertEquals(2,merged.getVersion());
        assertEquals(2,merged.getNodesCount());
        assertEquals(1,merged.getNode(0).getId());
        assertEquals(2,merged.getNode(1).getId());

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

        DataSet my = new DataSet();
        my.version = "0.6";

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

        DataSet their = new DataSet();
        their.version = "0.6";

        Node n3 = new Node(new LatLon(0,0));
        n3.setOsmId(1,1);
        their.addPrimitive(n3);

        Node n5 = new Node(new LatLon(1,1));
        n5.setOsmId(4,1);

        their.addPrimitive(n5);

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


        MergeVisitor visitor = new MergeVisitor(my,their);
        visitor.merge();

        Way merged = (Way)my.getPrimitiveById(3, OsmPrimitiveType.WAY);
        assertEquals(0,visitor.getConflicts().size());
        assertEquals(3,merged.getId());
        assertEquals(2,merged.getVersion());
        assertEquals(3,merged.getNodesCount());
        assertEquals(1,merged.getNode(0).getId());
        assertEquals(4,merged.getNode(1).getId());
        assertEquals(2,merged.getNode(2).getId());
        assertEquals("value1",merged.getNode(2).get("key1"));
    }

    /**
     * their way has a higher version and different nodes. My way is modified.
     *
     * => merge it onto my way not possbile, conflict
     *
     */
    @Test
    public void waySimple_DifferentNodesAndMyIsModified() {

        DataSet my = new DataSet();
        my.version = "0.6";

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

        DataSet their = new DataSet();
        their.version = "0.6";

        Node n3 = new Node(new LatLon(0,0));
        n3.setOsmId(1,1);
        their.addPrimitive(n3);

        Node n5 = new Node(new LatLon(1,1));
        n5.setOsmId(4,1);
        their.addPrimitive(n5);

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


        MergeVisitor visitor = new MergeVisitor(my,their);
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
    public void waySimple_TheirVersionNotVisible() {

        DataSet my = new DataSet();
        my.version = "0.6";

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

        DataSet their = new DataSet();
        their.version = "0.6";

        Way theirWay = new Way();
        theirWay.setOsmId(3,2);
        theirWay.setVisible(false);
        their.addPrimitive(theirWay);

        MergeVisitor visitor = new MergeVisitor(my,their);
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
     *
     *
     */
    @Test
    public void waySimple_twoWaysWithNoId_NodesWithId() {

        DataSet my = new DataSet();
        my.version = "0.6";

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

        DataSet their = new DataSet();
        their.version = "0.6";

        Node n3 = new Node(new LatLon(0,0));
        n3.setOsmId(1,1);
        their.addPrimitive(n3);

        Node n4 = new Node(new LatLon(1,1));
        n4.setOsmId(2,1);
        their.addPrimitive(n4);

        Way theirWay = new Way();
        theirWay.addNode(n3);
        theirWay.addNode(n4);
        theirWay.setUser(User.createOsmUser(1111, "their"));
        theirWay.setTimestamp(new Date());
        their.addPrimitive(theirWay);

        MergeVisitor visitor = new MergeVisitor(my,their);
        visitor.merge();

        assertEquals(0,visitor.getConflicts().size());
        assertEquals("their", myWay.getUser().getName());
        assertEquals(1111, myWay.getUser().getId());
        assertEquals(1111, myWay.getUser().getId());
        assertEquals(theirWay.getTimestamp(), myWay.getTimestamp());
    }

    /**
     * my and their way have no ids, neither do the nodes they refer to. but
     * my and  their way are semantically equal. so technical attributes of
     * their way can be merged on my way. No conflict.
     *
     */
    @Test
    public void waySimple_twoWaysWithNoId_NodesWithoutId() {

        DataSet my = new DataSet();
        my.version = "0.6";

        Node n1 = new Node(new LatLon(0,0));
        my.addPrimitive(n1);

        Node n2 = new Node(new LatLon(1,1));
        my.addPrimitive(n2);

        Way myWay = new Way();
        myWay.addNode(n1);
        myWay.addNode(n2);
        my.addPrimitive(myWay);

        DataSet their = new DataSet();
        their.version = "0.6";

        Node n3 = new Node(new LatLon(0,0));
        their.addPrimitive(n3);

        Node n4 = new Node(new LatLon(1,1));
        their.addPrimitive(n4);

        Way theirWay = new Way();
        theirWay.addNode(n3);
        theirWay.addNode(n4);
        theirWay.setUser(User.createOsmUser(1111, "their"));
        theirWay.setTimestamp(new Date());
        their.addPrimitive(theirWay);

        MergeVisitor visitor = new MergeVisitor(my,their);
        visitor.merge();

        assertEquals(0,visitor.getConflicts().size());
        assertEquals("their", myWay.getUser().getName());
        assertEquals(1111, myWay.getUser().getId());
        assertEquals(1111, myWay.getUser().getId());
        assertEquals(theirWay.getTimestamp(), myWay.getTimestamp());
    }


    /**
     * My dataset includes a deleted node.
     * Their dataset includes a way with three nodes, the first one being my node.
     *
     * => the merged way should include two nodes only. the deleted node should still be
     * in the data set
     *
     */
    @Test
    public void wayComplex_mergingADeletedNode() {

        DataSet my = new DataSet();
        my.version = "0.6";

        Node n1 = new Node(new LatLon(0,0));
        n1.setOsmId(1,1);
        n1.setDeleted(true);
        my.addPrimitive(n1);

        DataSet their = new DataSet();
        their.version = "0.6";

        Node n3 = new Node(new LatLon(0,0));
        n3.setOsmId(1,1);
        their.addPrimitive(n3);

        Node n4 = new Node(new LatLon(1,1));
        n4.setOsmId(2,1);
        their.addPrimitive(n4);

        Node n5 = new Node(new LatLon(2,2));
        n5.setOsmId(3,1);
        their.addPrimitive(n5);


        Way theirWay = new Way();
        theirWay.setOsmId(4,1);
        theirWay.addNode(n3);
        theirWay.addNode(n4);
        theirWay.addNode(n5);
        theirWay.setUser(User.createOsmUser(1111, "their"));
        theirWay.setTimestamp(new Date());
        their.addPrimitive(theirWay);

        MergeVisitor visitor = new MergeVisitor(my,their);
        visitor.merge();

        assertEquals(0,visitor.getConflicts().size());

        Way myWay = (Way)my.getPrimitiveById(4,OsmPrimitiveType.WAY);
        assertEquals(2, myWay.getNodesCount());

        Node n = (Node)my.getPrimitiveById(1,OsmPrimitiveType.NODE);
        assertTrue(n != null);
    }

    /**
     * My dataset includes a deleted node.
     * Their dataset includes a relation with thre nodes, the first one being my node.
     *
     * => the merged relation should include two nodes only. the deleted node should still be
     * in the data set
     *
     */
    @Test
    public void relationComplex_mergingADeletedNode() {

        DataSet my = new DataSet();
        my.version = "0.6";

        Node n1 = new Node(new LatLon(0,0));
        n1.setOsmId(1,1);
        n1.setDeleted(true);
        my.addPrimitive(n1);

        DataSet their = new DataSet();
        their.version = "0.6";

        Node n3 = new Node(new LatLon(0,0));
        n3.setOsmId(1,1);
        their.addPrimitive(n3);

        Node n4 = new Node(new LatLon(1,1));
        n4.setOsmId(2,1);
        their.addPrimitive(n4);

        Node n5 = new Node(new LatLon(2,2));
        n5.setOsmId(3,1);
        their.addPrimitive(n5);


        Relation theirRelation = new Relation();
        theirRelation.setOsmId(4,1);

        theirRelation.addMember(new RelationMember("", n3));
        theirRelation.addMember(new RelationMember("", n4));
        theirRelation.addMember(new RelationMember("", n5));
        their.addPrimitive(theirRelation);

        MergeVisitor visitor = new MergeVisitor(my,their);
        visitor.merge();

        assertEquals(0,visitor.getConflicts().size());

        Relation r = (Relation)my.getPrimitiveById(4,OsmPrimitiveType.RELATION);
        assertEquals(2, r.getMembersCount());

        Node n = (Node)my.getPrimitiveById(1,OsmPrimitiveType.NODE);
        assertTrue(n != null);
    }

    /**
     * Merge an incomplete way with two incomplete nodes into an empty dataset.
     * 
     * Use case: a way loaded with a multiget, i.e. GET /api/0.6/ways?ids=123456
     */
    @Test
    public void newIncompleteWay() {
        DataSet their = new DataSet();
        their.version = "0.6";

        Node n1 = new Node(1);
        their.addPrimitive(n1);

        Node n2 = new Node(2);
        their.addPrimitive(n2);

        Way w3 = new Way(3);
        w3.incomplete = true;
        w3.setNodes(Arrays.asList(n1,n2));
        their.addPrimitive(w3);

        DataSet my = new DataSet();
        their.version = "0.6";

        MergeVisitor visitor = new MergeVisitor(my,their);
        visitor.merge();

        assertEquals(0,visitor.getConflicts().size());

        OsmPrimitive p= my.getPrimitiveById(1, OsmPrimitiveType.NODE);
        assertNotNull(p);
        assertTrue(p.incomplete);
        p= my.getPrimitiveById(2, OsmPrimitiveType.NODE);
        assertNotNull(p);
        assertTrue(p.incomplete);
        p= my.getPrimitiveById(3, OsmPrimitiveType.WAY);
        assertNotNull(p);
        assertTrue(p.incomplete);

        Way w = (Way)my.getPrimitiveById(3, OsmPrimitiveType.WAY);
        assertNotNull(w);
        assertTrue(p.incomplete);
        assertEquals(2, w.getNodesCount());
        assertTrue(w.getNode(0).incomplete);
        assertTrue(w.getNode(1).incomplete);
    }

    /**
     * Merge an incomplete way with two incomplete nodes into a dataset where the way already exists as complete way.
     * 
     * Use case: a way loaded with a multiget, i.e. GET /api/0.6/ways?ids=123456 after a "Update selection " of this way
     */
    @Test
    public void incompleteWayOntoCompleteWay() {
        DataSet their = new DataSet();
        their.version = "0.6";

        // an incomplete node
        Node n1 = new Node(1);
        their.addPrimitive(n1);

        // another incomplete node
        Node n2 = new Node(2);
        their.addPrimitive(n2);

        // an incomplete way with two incomplete nodes
        Way w3 = new Way(3);
        w3.incomplete = true;
        w3.setNodes(Arrays.asList(n1,n2));
        their.addPrimitive(w3);

        DataSet my = new DataSet();
        their.version = "0.6";

        Node n4 = new Node(new LatLon(0,0));
        n4.setOsmId(1,1);
        my.addPrimitive(n4);

        Node n5 = new Node(new LatLon(1,1));
        n5.setOsmId(2,1);
        my.addPrimitive(n5);

        Way w6 = new Way(3);
        w6.incomplete = false;
        w6.setNodes(Arrays.asList(n4,n5));
        my.addPrimitive(w6);

        MergeVisitor visitor = new MergeVisitor(my,their);
        visitor.merge();

        assertEquals(0,visitor.getConflicts().size());

        OsmPrimitive p= my.getPrimitiveById(1, OsmPrimitiveType.NODE);
        assertNotNull(p);
        assertTrue(!p.incomplete);
        p= my.getPrimitiveById(2, OsmPrimitiveType.NODE);
        assertNotNull(p);
        assertTrue(!p.incomplete);
        p= my.getPrimitiveById(3,OsmPrimitiveType.WAY);
        assertNotNull(p);
        assertTrue(!p.incomplete);

        Way w = (Way)my.getPrimitiveById(3,OsmPrimitiveType.WAY);
        assertNotNull(w);
        assertTrue(!p.incomplete);
        assertEquals(2, w.getNodesCount());
        assertTrue(!w.getNode(0).incomplete);
        assertTrue(!w.getNode(1).incomplete);
    }
}
