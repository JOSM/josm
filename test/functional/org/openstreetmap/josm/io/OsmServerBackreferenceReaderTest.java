// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.projection.Mercator;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.xml.sax.SAXException;

public class OsmServerBackreferenceReaderTest {
    static private final Logger logger = Logger.getLogger(OsmServerBackreferenceReader.class.getName());

    protected static Node lookupNode(DataSet ds, int i) {
        for (Node n: ds.nodes) {
            if (("node-" + i).equals(n.get("name"))) return n;
        }
        return null;
    }


    protected static Way lookupWay(DataSet ds, int i) {
        for (Way w: ds.ways) {
            if (("way-" + i).equals(w.get("name"))) return w;
        }
        return null;
    }

    protected static Relation lookupRelation(DataSet ds, int i) {
        for (Relation r: ds.relations) {
            if (("relation-" + i).equals(r.get("name"))) return r;
        }
        return null;
    }

    protected static void populateTestDataSetWithNodes(DataSet ds) {
        for (int i=0;i<100;i++) {
            Node n = new Node(0);
            n.setCoor(new LatLon(-36.6,47.6));
            n.put("name", "node-"+i);
            n.incomplete = false;
            ds.addPrimitive(n);
        }
    }

    protected static void populateTestDataSetWithWays(DataSet ds) {
        for (int i=0;i<20;i++) {
            Way w = new Way(0);
            w.incomplete = false;
            for (int j = 0; j < 10;j++) {
                w.addNode(lookupNode(ds, i+j));
            }
            w.put("name", "way-"+i);
            ds.addPrimitive(w);
        }
    }

    protected static void populateTestDataSetWithRelations(DataSet ds) {
        for (int i=0;i<10;i++) {
            Relation r = new Relation(0);
            r.incomplete = false;
            r.put("name", "relation-" +i);
            for (int j =0; j < 10; j++) {
                RelationMember member = new RelationMember("node-" + j, lookupNode(ds, i + j));
                r.addMember(member);
            }
            for (int j =0; j < 5; j++) {
                RelationMember member = new RelationMember("way-" + j, lookupWay(ds, i + j));
                r.addMember(member);
            }
            if (i > 5) {
                for (int j =0; j < 3; j++) {
                    RelationMember member = new RelationMember("relation-" + j, lookupRelation(ds, j));
                    logger.info(MessageFormat.format("adding relation {0} to relation {1}", j, i));
                    r.addMember(member);
                }
            }
            ds.relations.add(r);
        }
    }


    protected static DataSet buildTestDataSet() {
        DataSet ds = new DataSet();
        ds.version = "0.6";

        populateTestDataSetWithNodes(ds);
        populateTestDataSetWithWays(ds);
        populateTestDataSetWithRelations(ds);
        return ds;
    }

    /**
     * creates the dataset on the server.
     *
     * @param ds the data set
     * @throws OsmTransferException
     */
    static public void createDataSetOnServer(DataSet ds) throws OsmTransferException {
        logger.info("creating data set on the server ...");
        ArrayList<OsmPrimitive> primitives = new ArrayList<OsmPrimitive>();
        primitives.addAll(ds.nodes);
        primitives.addAll(ds.ways);
        primitives.addAll(ds.relations);
        OsmServerWriter writer = new OsmServerWriter();
        writer.uploadOsm("0.6", primitives, null, null, NullProgressMonitor.INSTANCE);
    }

    static Properties testProperties;
    static DataSet testDataSet;

    @BeforeClass
    public static void  init() throws OsmTransferException, InterruptedException{
        logger.info("initializing ...");
        testProperties = new Properties();

        // load properties
        //
        try {
            testProperties.load(MultiFetchServerObjectReaderTest.class.getResourceAsStream("/test-functional-env.properties"));
        } catch(Exception e){
            logger.log(Level.SEVERE, MessageFormat.format("failed to load property file ''{0}''", "test-functional-env.properties"));
            fail(MessageFormat.format("failed to load property file ''{0}''", "test-functional-env.properties"));
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

        // check temp output dir
        //
        String tempOutputDir = testProperties.getProperty("test.functional.tempdir");
        if (tempOutputDir == null) {
            fail(MessageFormat.format("property ''{0}'' not set in test environment", "test.functional.tempdir"));
        } else {
            File f = new File(tempOutputDir);
            if (! f.exists() || ! f.isDirectory() || ! f.canWrite()) {
                fail(MessageFormat.format("property ''{0}'' points to ''{1}'' which is either not existing, not a directory, or not writeable", "test.functional.tempdir", tempOutputDir));
            }
        }


        // init preferences
        //
        System.setProperty("josm.home", josmHome);
        Main.pref.init(false);
        // don't use atomic upload, the test API server can't cope with large diff uploads
        //
        Main.pref.put("osm-server.atomic-upload", false);
        Main.proj = new Mercator();

        File dataSetCacheOutputFile = new File(tempOutputDir, MultiFetchServerObjectReaderTest.class.getName() + ".dataset");

        // make sure we don't upload to production
        //
        String url = OsmApi.getOsmApi().getBaseUrl().toLowerCase().trim();
        if (url.startsWith("http://www.openstreetmap.org")
                || url.startsWith("http://api.openstreetmap.org")) {
            fail(MessageFormat.format("configured url ''{0}'' seems to be a productive url, aborting.", url));
        }


        String p = System.getProperties().getProperty("useCachedDataset");
        if (p != null && Boolean.parseBoolean(p.trim().toLowerCase())) {
            logger.info(MessageFormat.format("property ''{0}'' set, using cached dataset", "useCachedDataset"));
            return;
        }

        logger.info(MessageFormat.format("property ''{0}'' not set to true, creating test dataset on the server. property is ''{1}''", "useCachedDataset", p));

        // build and upload the test data set
        //
        logger.info("creating test data set ....");
        testDataSet = buildTestDataSet();
        logger.info("uploading test data set ...");
        createDataSetOnServer(testDataSet);

        PrintWriter pw = null;
        try {
            pw = new PrintWriter(
                    new FileWriter(dataSetCacheOutputFile)
            );
        } catch(IOException e) {
            fail(MessageFormat.format("failed to open file ''{0}'' for writing", dataSetCacheOutputFile.toString()));
        }
        logger.info(MessageFormat.format("caching test data set in ''{0}'' ...", dataSetCacheOutputFile.toString()));
        OsmWriter w = new OsmWriter(pw, false, testDataSet.version);
        w.header();
        w.writeDataSources(testDataSet);
        w.writeContent(testDataSet);
        w.footer();
        w.close();
        pw.close();
    }

    private DataSet ds;

    @Before
    public void setUp() throws IOException, IllegalDataException {
        File f = new File(testProperties.getProperty("test.functional.tempdir"), MultiFetchServerObjectReaderTest.class.getName() + ".dataset");
        logger.info(MessageFormat.format("reading cached dataset ''{0}''", f.toString()));
        ds = new DataSet();
        FileInputStream fis = new FileInputStream(f);
        ds = OsmReader.parseDataSet(fis, NullProgressMonitor.INSTANCE);
        fis.close();
    }

    @Test
    public void testBackrefrenceForNode() throws OsmTransferException {
        Node n = lookupNode(ds, 0);
        assertNotNull(n);
        Way w = lookupWay(ds, 0);
        assertNotNull(w);

        OsmServerBackreferenceReader reader = new OsmServerBackreferenceReader(n);
        reader.setReadFull(false);
        DataSet referers = reader.parseOsm(NullProgressMonitor.INSTANCE);
        assertEquals(10, referers.nodes.size());
        assertEquals(1, referers.ways.size());
        assertEquals(0, referers.relations.size());
        for (Way way: referers.ways) {
            assertEquals(w.id, way.id);
            assertEquals(false, way.incomplete);
        }
    }

    @Test
    public void testBackrefrenceForNode_Full() throws OsmTransferException {
        Node n = lookupNode(ds, 0);
        assertNotNull(n);
        Way w = lookupWay(ds, 0);
        assertNotNull(w);

        OsmServerBackreferenceReader reader = new OsmServerBackreferenceReader(n);
        reader.setReadFull(true);
        DataSet referers = reader.parseOsm(NullProgressMonitor.INSTANCE);
        assertEquals(10, referers.nodes.size());
        assertEquals(1, referers.ways.size());
        assertEquals(0, referers.relations.size());
        for (Way way: referers.ways) {
            assertEquals(w.id, way.id);
            assertEquals(false, way.incomplete);
            assertEquals(10, w.getNodesCount());
        }
    }

    @Test
    public void testBackrefrenceForWay() throws OsmTransferException {
        Way w = lookupWay(ds, 1);
        assertNotNull(w);
        // way with name "way-1" is referred to by two relations
        //

        OsmServerBackreferenceReader reader = new OsmServerBackreferenceReader(w);
        reader.setReadFull(false);
        DataSet referers = reader.parseOsm(NullProgressMonitor.INSTANCE);
        assertEquals(0, referers.nodes.size()); // no nodes loaded
        assertEquals(6, referers.ways.size());  // 6 ways referred by two relations
        for (Way w1: referers.ways) {
            assertEquals(true, w1.incomplete);
        }
        assertEquals(2, referers.relations.size());  // two relations referring to w

        Relation r = lookupRelation(referers, 0);
        assertNotNull(r);
        assertEquals(false, r.incomplete);
        r = lookupRelation(referers, 1);
        assertEquals(false, r.incomplete);
    }

    @Test
    public void testBackrefrenceForWay_Full() throws OsmTransferException {
        Way w = lookupWay(ds, 1);
        assertNotNull(w);
        // way with name "way-1" is referred to by two relations
        //

        OsmServerBackreferenceReader reader = new OsmServerBackreferenceReader(w);
        reader.setReadFull(true);
        DataSet referers = reader.parseOsm(NullProgressMonitor.INSTANCE);
        assertEquals(6, referers.ways.size());  // 6 ways referred by two relations
        for (Way w1: referers.ways) {
            assertEquals(false, w1.incomplete);
        }
        assertEquals(2, referers.relations.size());  // two relations referring to
        Set<Long> expectedNodeIds = new HashSet<Long>();
        for (Way way: referers.ways) {
            Way orig = (Way)ds.getPrimitiveById(way.id);
            for(Node n: orig.getNodes()) {
                expectedNodeIds.add(n.id);
            }
        }
        assertEquals(expectedNodeIds.size(), referers.nodes.size());
        for (Node n : referers.nodes) {
            assertEquals(true, expectedNodeIds.contains(n.id));
        }

        Relation r = lookupRelation(referers, 0);
        assertNotNull(r);
        assertEquals(false, r.incomplete);
        r = lookupRelation(referers, 1);
        assertEquals(false, r.incomplete);
    }

    @Test
    public void testBackrefrenceForRelation() throws OsmTransferException {
        Relation r = lookupRelation(ds,1);
        assertNotNull(r);
        // way with name "relation-1" is referred to by four relations:
        //    relation-6, relation-7, relation-8, relation-9
        //

        OsmServerBackreferenceReader reader = new OsmServerBackreferenceReader(r);
        reader.setReadFull(false);
        DataSet referers = reader.parseOsm(NullProgressMonitor.INSTANCE);

        Set<Long> referringRelationsIds = new HashSet<Long>();
        r = lookupRelation(referers, 6);
        assertNotNull(r);
        assertEquals(false, r.incomplete);
        referringRelationsIds.add(r.id);
        r = lookupRelation(referers, 7);
        assertNotNull(r);
        assertEquals(false, r.incomplete);
        referringRelationsIds.add(r.id);
        r = lookupRelation(referers, 8);
        assertNotNull(r);
        assertEquals(false, r.incomplete);
        referringRelationsIds.add(r.id);
        r = lookupRelation(referers, 9);
        assertNotNull(r);
        assertEquals(false, r.incomplete);
        referringRelationsIds.add(r.id);

        for (Relation r1: referers.relations) {
            if (! referringRelationsIds.contains(r1.id)) {
                assertEquals(true, r1.incomplete);
            }
        }

        // make sure we read all ways referred to by parent relations. These
        // ways are incomplete after reading.
        //
        Set<Long> expectedWayIds = new HashSet<Long>();
        for (RelationMember m : lookupRelation(ds, 6).getMembers()) {
            if (m.isWay()) {
                expectedWayIds.add(m.getMember().id);
            }
        }
        for (RelationMember m : lookupRelation(ds, 7).getMembers()) {
            if (m.isWay()) {
                expectedWayIds.add(m.getMember().id);
            }
        }
        for (RelationMember m : lookupRelation(ds, 8).getMembers()) {
            if (m.isWay()) {
                expectedWayIds.add(m.getMember().id);
            }
        }
        for (RelationMember m : lookupRelation(ds, 9).getMembers()) {
            if (m.isWay()) {
                expectedWayIds.add(m.getMember().id);
            }
        }

        assertEquals(expectedWayIds.size(), referers.ways.size());
        for (Way w1 : referers.ways) {
            assertEquals(true, expectedWayIds.contains(w1.id));
            assertEquals(true, w1.incomplete);
        }

        // make sure we didn't read any nodes
        //
        assertEquals(0, referers.nodes.size());
    }

    protected Set<Long> getNodeIdsInWay(Way way) {
        HashSet<Long> ret = new HashSet<Long>();
        if (way == null)return ret;
        for (Node n: way.getNodes()) {
            ret.add(n.id);
        }
        return ret;
    }

    protected Set<Long> getNodeIdsInRelation(Relation r) {
        HashSet<Long> ret = new HashSet<Long>();
        if (r == null) return ret;
        for (RelationMember m: r.getMembers()) {
            if (m.isNode()) {
                ret.add(m.getMember().id);
            } else if (m.isWay()) {
                ret.addAll(getNodeIdsInWay(m.getWay()));
            } else if (m.isRelation()) {
                ret.addAll(getNodeIdsInRelation(m.getRelation()));
            }
        }
        return ret;
    }

    @Test
    public void testBackrefrenceForRelation_Full() throws OsmTransferException {
        Relation r = lookupRelation(ds,1);
        assertNotNull(r);
        // way with name "relation-1" is referred to by four relations:
        //    relation-6, relation-7, relation-8, relation-9
        //

        OsmServerBackreferenceReader reader = new OsmServerBackreferenceReader(r);
        reader.setReadFull(true);
        DataSet referers = reader.parseOsm(NullProgressMonitor.INSTANCE);

        Set<Long> referringRelationsIds = new HashSet<Long>();
        r = lookupRelation(referers, 6);
        assertNotNull(r);
        assertEquals(false, r.incomplete);
        referringRelationsIds.add(r.id);
        r = lookupRelation(referers, 7);
        assertNotNull(r);
        assertEquals(false, r.incomplete);
        referringRelationsIds.add(r.id);
        r = lookupRelation(referers, 8);
        assertNotNull(r);
        assertEquals(false, r.incomplete);
        referringRelationsIds.add(r.id);
        r = lookupRelation(referers, 9);
        assertNotNull(r);
        assertEquals(false, r.incomplete);
        referringRelationsIds.add(r.id);

        // all relations are fully loaded
        //
        for (Relation r1: referers.relations) {
            assertEquals(false, r1.incomplete);
        }

        // make sure we read all ways referred to by parent relations. These
        // ways are completely read after reading the relations
        //
        Set<Long> expectedWayIds = new HashSet<Long>();
        for (RelationMember m : lookupRelation(ds, 6).getMembers()) {
            if (m.isWay()) {
                expectedWayIds.add(m.getMember().id);
            }
        }
        for (RelationMember m : lookupRelation(ds, 7).getMembers()) {
            if (m.isWay()) {
                expectedWayIds.add(m.getMember().id);
            }
        }
        for (RelationMember m : lookupRelation(ds, 8).getMembers()) {
            if (m.isWay()) {
                expectedWayIds.add(m.getMember().id);
            }
        }
        for (RelationMember m : lookupRelation(ds, 9).getMembers()) {
            if (m.isWay()) {
                expectedWayIds.add(m.getMember().id);
            }
        }
        for (long id : expectedWayIds) {
            Way w = (Way)referers.getPrimitiveById(id);
            assertNotNull(w);
            assertEquals(false, w.incomplete);
        }

        Set<Long> expectedNodeIds = new HashSet<Long>();
        for(int i=6; i< 10;i++) {
            Relation r1 = lookupRelation(ds, i);
            expectedNodeIds.addAll(getNodeIdsInRelation(r1));
        }

        assertEquals(expectedNodeIds.size(), referers.nodes.size());
        for(Node n : referers.nodes) {
            assertEquals(true, expectedNodeIds.contains(n.id));
        }
    }
}
