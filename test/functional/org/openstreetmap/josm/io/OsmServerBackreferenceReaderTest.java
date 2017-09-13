// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.APIDataSet;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.CyclicUploadDependencyException;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.tools.Logging;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Reads primitives referring to a particular primitive (ways including a node, relations referring to a relation)
 * @since 1806
 */
@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS")
public class OsmServerBackreferenceReaderTest {
    private static final Logger logger = Logger.getLogger(OsmServerBackreferenceReader.class.getName());

    protected static Node lookupNode(DataSet ds, int i) {
        for (Node n : ds.getNodes()) {
            if (("node-" + i).equals(n.get("name"))) return n;
        }
        fail("Cannot find node "+i);
        return null;
    }

    protected static Way lookupWay(DataSet ds, int i) {
        for (Way w : ds.getWays()) {
            if (("way-" + i).equals(w.get("name"))) return w;
        }
        fail("Cannot find way "+i);
        return null;
    }

    protected static Relation lookupRelation(DataSet ds, int i) {
        for (Relation r : ds.getRelations()) {
            if (("relation-" + i).equals(r.get("name"))) return r;
        }
        fail("Cannot find relation "+i);
        return null;
    }

    protected static void populateTestDataSetWithNodes(DataSet ds) {
        for (int i = 0; i < 100; i++) {
            Node n = new Node();
            n.setCoor(new LatLon(-36.6, 47.6));
            n.put("name", "node-"+i);
            ds.addPrimitive(n);
        }
    }

    protected static void populateTestDataSetWithWays(DataSet ds) {
        for (int i = 0; i < 20; i++) {
            Way w = new Way();
            for (int j = 0; j < 10; j++) {
                w.addNode(lookupNode(ds, i+j));
            }
            w.put("name", "way-"+i);
            ds.addPrimitive(w);
        }
    }

    protected static void populateTestDataSetWithRelations(DataSet ds) {
        for (int i = 0; i < 10; i++) {
            Relation r = new Relation();
            r.put("name", "relation-" +i);
            for (int j = 0; j < 10; j++) {
                RelationMember member = new RelationMember("node-" + j, lookupNode(ds, i + j));
                r.addMember(member);
            }
            for (int j = 0; j < 5; j++) {
                RelationMember member = new RelationMember("way-" + j, lookupWay(ds, i + j));
                r.addMember(member);
            }
            if (i > 5) {
                for (int j = 0; j < 3; j++) {
                    RelationMember member = new RelationMember("relation-" + j, lookupRelation(ds, j));
                    logger.info(MessageFormat.format("adding relation {0} to relation {1}", j, i));
                    r.addMember(member);
                }
            }
            ds.addPrimitive(r);
        }
    }

    protected static DataSet buildTestDataSet() {
        DataSet ds = new DataSet();
        ds.setVersion("0.6");

        populateTestDataSetWithNodes(ds);
        populateTestDataSetWithWays(ds);
        populateTestDataSetWithRelations(ds);
        return ds;
    }

    /**
     * creates the dataset on the server.
     *
     * @param ds the data set
     * @throws OsmTransferException if something goes wrong
     * @throws CyclicUploadDependencyException if a cyclic dependency is detected
     */
    public static void createDataSetOnServer(APIDataSet ds) throws OsmTransferException, CyclicUploadDependencyException {
        logger.info("creating data set on the server ...");
        ds.adjustRelationUploadOrder();
        OsmServerWriter writer = new OsmServerWriter();
        Changeset cs = new Changeset();
        writer.uploadOsm(
                new UploadStrategySpecification().setStrategy(UploadStrategy.SINGLE_REQUEST_STRATEGY),
                ds.getPrimitives(), cs, NullProgressMonitor.INSTANCE);
        OsmApi.getOsmApi().closeChangeset(cs, NullProgressMonitor.INSTANCE);
    }

    static DataSet testDataSet;

    /**
     * Setup test.
     * @throws OsmTransferException if something goes wrong
     * @throws CyclicUploadDependencyException if a cyclic dependency is detected
     * @throws IOException if an I/O error occurs
     */
    @BeforeClass
    public static void setUpBeforeClass() throws OsmTransferException, CyclicUploadDependencyException, IOException {
        logger.info("initializing ...");

        JOSMFixture.createFunctionalTestFixture().init();

        Main.pref.put("osm-server.auth-method", "basic");

        // don't use atomic upload, the test API server can't cope with large diff uploads
        //
        Main.pref.putBoolean("osm-server.atomic-upload", false);
        Main.setProjection(Projections.getProjectionByCode("EPSG:3857")); // Mercator
        Logging.setLogLevel(Logging.LEVEL_DEBUG);

        File dataSetCacheOutputFile = new File(System.getProperty("java.io.tmpdir"),
                MultiFetchServerObjectReaderTest.class.getName() + ".dataset");

        String p = System.getProperty("useCachedDataset");
        if (p != null && Boolean.parseBoolean(p.trim().toLowerCase(Locale.ENGLISH))) {
            logger.info(MessageFormat.format("property ''{0}'' set, using cached dataset", "useCachedDataset"));
            return;
        }

        logger.info(MessageFormat.format(
                "property ''{0}'' not set to true, creating test dataset on the server. property is ''{1}''", "useCachedDataset", p));

        // build and upload the test data set
        //
        logger.info("creating test data set ....");
        testDataSet = buildTestDataSet();
        logger.info("uploading test data set ...");
        createDataSetOnServer(new APIDataSet(testDataSet));

        try (
            PrintWriter pw = new PrintWriter(
                    new OutputStreamWriter(new FileOutputStream(dataSetCacheOutputFile), StandardCharsets.UTF_8)
        )) {
            logger.info(MessageFormat.format("caching test data set in ''{0}'' ...", dataSetCacheOutputFile.toString()));
            try (OsmWriter w = new OsmWriter(pw, false, testDataSet.getVersion())) {
                w.header();
                w.writeDataSources(testDataSet);
                w.writeContent(testDataSet);
                w.footer();
            }
        }
    }

    private DataSet ds;

    /**
     * Setup test.
     * @throws IOException if any I/O error occurs
     * @throws IllegalDataException if an error was found while parsing the OSM data
     * @throws FileNotFoundException if the dataset file cannot be found
     */
    @Before
    public void setUp() throws IOException, IllegalDataException, FileNotFoundException {
        File f = new File(System.getProperty("java.io.tmpdir"), MultiFetchServerObjectReaderTest.class.getName() + ".dataset");
        logger.info(MessageFormat.format("reading cached dataset ''{0}''", f.toString()));
        ds = new DataSet();
        try (FileInputStream fis = new FileInputStream(f)) {
            ds = OsmReader.parseDataSet(fis, NullProgressMonitor.INSTANCE);
        }
    }

    /**
     * Test reading references for a node.
     * @throws OsmTransferException if an error occurs
     */
    @Test
    public void testBackreferenceForNode() throws OsmTransferException {
        Node n = lookupNode(ds, 0);
        assertNotNull(n);
        Way w = lookupWay(ds, 0);
        assertNotNull(w);

        OsmServerBackreferenceReader reader = new OsmServerBackreferenceReader(n);
        reader.setReadFull(false);
        DataSet referers = reader.parseOsm(NullProgressMonitor.INSTANCE);
        printNumberOfPrimitives(referers);

        Set<Long> expectedNodeIds = new HashSet<>();
        Set<Long> expectedWayIds = new HashSet<>();
        Set<Long> expectedRelationIds = new HashSet<>();

        for (OsmPrimitive ref : n.getReferrers()) {
            if (ref instanceof Way) {
                expectedWayIds.add(ref.getId());
                expectedNodeIds.addAll(getNodeIdsInWay((Way) ref));
            } else if (ref instanceof Relation) {
                expectedRelationIds.add(ref.getId());
                expectedWayIds.addAll(getWayIdsInRelation((Relation) ref, false));
                expectedNodeIds.addAll(getNodeIdsInRelation((Relation) ref, false));
            }
        }

        assertEquals(expectedNodeIds.size(), referers.getNodes().size());
        assertEquals(expectedWayIds.size(), referers.getWays().size());
        assertEquals(expectedRelationIds.size(), referers.getRelations().size());

        for (Node node : referers.getNodes()) {
            assertTrue(expectedNodeIds.contains(node.getId()));
            assertFalse(node.isIncomplete());
        }

        for (Way way : referers.getWays()) {
            assertTrue(expectedWayIds.contains(way.getId()));
            assertEquals(n.getReferrers().contains(way), !way.isIncomplete());
        }

        for (Relation relation : referers.getRelations()) {
            assertTrue(expectedRelationIds.contains(relation.getId()));
            assertFalse(relation.isIncomplete());
        }
    }

    private void printNumberOfPrimitives(DataSet referers) {
        System.out.println("#nodes=" + referers.getNodes().size() +
                " #ways=" + referers.getWays().size() +
                " #relations=" + referers.getRelations().size());
    }

    /**
     * Test reading full references for a node.
     * @throws OsmTransferException if an error occurs
     */
    @Test
    public void testBackreferenceForNodeFull() throws OsmTransferException {
        Node n = lookupNode(ds, 0);
        assertNotNull(n);

        OsmServerBackreferenceReader reader = new OsmServerBackreferenceReader(n);
        reader.setReadFull(true);
        DataSet referers = reader.parseOsm(NullProgressMonitor.INSTANCE);
        printNumberOfPrimitives(referers);

        Set<Long> expectedNodeIds = new HashSet<>();
        Set<Long> expectedWayIds = new HashSet<>();
        Set<Long> expectedRelationIds = new HashSet<>();
        for (OsmPrimitive ref : n.getReferrers()) {
            if (ref instanceof Way) {
                expectedWayIds.add(ref.getId());
                expectedNodeIds.addAll(getNodeIdsInWay((Way) ref));
            } else if (ref instanceof Relation) {
                expectedRelationIds.add(ref.getId());
                expectedWayIds.addAll(getWayIdsInRelation((Relation) ref, true));
                expectedNodeIds.addAll(getNodeIdsInRelation((Relation) ref, true));
            }
        }

        assertEquals(expectedNodeIds.size(), referers.getNodes().size());
        assertEquals(expectedWayIds.size(), referers.getWays().size());
        assertEquals(expectedRelationIds.size(), referers.getRelations().size());

        for (Node node : referers.getNodes()) {
            assertTrue(expectedNodeIds.contains(node.getId()));
            assertFalse(node.isIncomplete());
        }

        for (Way way : referers.getWays()) {
            assertTrue(expectedWayIds.contains(way.getId()));
            assertFalse(way.isIncomplete());
        }

        for (Relation relation : referers.getRelations()) {
            assertTrue(expectedRelationIds.contains(relation.getId()));
            assertFalse(relation.isIncomplete());
        }
    }

    /**
     * Test reading references for a way.
     * @throws OsmTransferException if an error occurs
     */
    @Test
    public void testBackreferenceForWay() throws OsmTransferException {
        Way w = lookupWay(ds, 1);
        assertNotNull(w);
        // way with name "way-1" is referred to by two relations
        //

        OsmServerBackreferenceReader reader = new OsmServerBackreferenceReader(w);
        reader.setReadFull(false);
        DataSet referers = reader.parseOsm(NullProgressMonitor.INSTANCE);
        printNumberOfPrimitives(referers);

        Set<Long> expectedNodeIds = new HashSet<>();
        Set<Long> expectedWayIds = new HashSet<>();
        Set<Long> expectedRelationIds = new HashSet<>();

        for (OsmPrimitive ref : w.getReferrers()) {
            if (ref instanceof Relation) {
                expectedRelationIds.add(ref.getId());
                expectedWayIds.addAll(getWayIdsInRelation((Relation) ref, false));
                expectedNodeIds.addAll(getNodeIdsInRelation((Relation) ref, false));
            }
        }

        assertEquals(expectedNodeIds.size(), referers.getNodes().size());
        assertEquals(expectedWayIds.size(), referers.getWays().size());
        assertEquals(expectedRelationIds.size(), referers.getRelations().size());

        for (Way w1 : referers.getWays()) {
            assertTrue(w1.isIncomplete());
        }
        assertEquals(2, referers.getRelations().size());  // two relations referring to w

        Relation r = lookupRelation(referers, 0);
        assertNotNull(r);
        assertFalse(r.isIncomplete());
        r = lookupRelation(referers, 1);
        assertFalse(r.isIncomplete());
    }

    /**
     * Test reading full references for a way.
     * @throws OsmTransferException if an error occurs
     */
    @Test
    public void testBackreferenceForWayFull() throws OsmTransferException {
        Way w = lookupWay(ds, 1);
        assertNotNull(w);
        // way with name "way-1" is referred to by two relations
        //

        OsmServerBackreferenceReader reader = new OsmServerBackreferenceReader(w);
        reader.setReadFull(true);
        DataSet referers = reader.parseOsm(NullProgressMonitor.INSTANCE);
        assertEquals(6, referers.getWays().size());  // 6 ways referred by two relations
        for (Way w1 : referers.getWays()) {
            assertFalse(w1.isIncomplete());
        }
        assertEquals(2, referers.getRelations().size());  // two relations referring to
        Set<Long> expectedNodeIds = new HashSet<>();
        for (Way way : referers.getWays()) {
            Way orig = (Way) ds.getPrimitiveById(way);
            for (Node n : orig.getNodes()) {
                expectedNodeIds.add(n.getId());
            }
        }
        assertEquals(expectedNodeIds.size(), referers.getNodes().size());
        for (Node n : referers.getNodes()) {
            assertTrue(expectedNodeIds.contains(n.getId()));
        }

        Relation r = lookupRelation(referers, 0);
        assertNotNull(r);
        assertFalse(r.isIncomplete());
        r = lookupRelation(referers, 1);
        assertFalse(r.isIncomplete());
    }

    /**
     * Test reading references for a relation.
     * @throws OsmTransferException if an error occurs
     */
    @Test
    public void testBackreferenceForRelation() throws OsmTransferException {
        Relation r = lookupRelation(ds, 1);
        assertNotNull(r);
        // way with name "relation-1" is referred to by four relations:
        //    relation-6, relation-7, relation-8, relation-9
        //

        OsmServerBackreferenceReader reader = new OsmServerBackreferenceReader(r);
        reader.setReadFull(false);
        DataSet referers = reader.parseOsm(NullProgressMonitor.INSTANCE);
        printNumberOfPrimitives(referers);

        Set<Long> referringRelationsIds = new HashSet<>();
        Relation r6 = lookupRelation(referers, 6);
        assertNotNull(r6);
        assertFalse(r6.isIncomplete());
        referringRelationsIds.add(r6.getId());
        Relation r7 = lookupRelation(referers, 7);
        assertNotNull(r7);
        assertFalse(r7.isIncomplete());
        referringRelationsIds.add(r7.getId());
        Relation r8 = lookupRelation(referers, 8);
        assertNotNull(r8);
        assertFalse(r8.isIncomplete());
        referringRelationsIds.add(r8.getId());
        Relation r9 = lookupRelation(referers, 9);
        assertNotNull(r9);
        assertFalse(r9.isIncomplete());
        referringRelationsIds.add(r9.getId());

        for (Relation r1 : referers.getRelations()) {
            if (!referringRelationsIds.contains(r1.getId())) {
                assertTrue(r1.isIncomplete());
            }
        }

        // make sure we read all ways referred to by parent relations. These
        // ways are incomplete after reading.
        //
        Set<Long> expectedWayIds = new HashSet<>();
        for (RelationMember m : lookupRelation(ds, 6).getMembers()) {
            if (m.isWay()) {
                expectedWayIds.add(m.getMember().getId());
            }
        }
        for (RelationMember m : lookupRelation(ds, 7).getMembers()) {
            if (m.isWay()) {
                expectedWayIds.add(m.getMember().getId());
            }
        }
        for (RelationMember m : lookupRelation(ds, 8).getMembers()) {
            if (m.isWay()) {
                expectedWayIds.add(m.getMember().getId());
            }
        }
        for (RelationMember m : lookupRelation(ds, 9).getMembers()) {
            if (m.isWay()) {
                expectedWayIds.add(m.getMember().getId());
            }
        }

        assertEquals(expectedWayIds.size(), referers.getWays().size());
        for (Way w1 : referers.getWays()) {
            assertTrue(expectedWayIds.contains(w1.getId()));
            assertTrue(w1.isIncomplete());
        }

        // make sure we read all nodes referred to by parent relations.
        Set<Long> expectedNodeIds = new HashSet<>();
        for (OsmPrimitive ref : r.getReferrers()) {
            if (ref instanceof Relation) {
                expectedNodeIds.addAll(getNodeIdsInRelation((Relation) ref, false));
            }
        }
        assertEquals(expectedNodeIds.size(), referers.getNodes().size());
    }

    protected static Set<Long> getNodeIdsInWay(Way way) {
        HashSet<Long> ret = new HashSet<>();
        if (way == null) return ret;
        for (Node n: way.getNodes()) {
            ret.add(n.getId());
        }
        return ret;
    }

    protected static Set<Long> getNodeIdsInRelation(Relation r, boolean children) {
        HashSet<Long> ret = new HashSet<>();
        if (r == null) return ret;
        for (RelationMember m: r.getMembers()) {
            if (m.isNode()) {
                ret.add(m.getMember().getId());
            } else if (m.isWay() && children) {
                ret.addAll(getNodeIdsInWay(m.getWay()));
            } else if (m.isRelation() && children) {
                ret.addAll(getNodeIdsInRelation(m.getRelation(), true));
            }
        }
        return ret;
    }

    protected static Set<Long> getWayIdsInRelation(Relation r, boolean children) {
        HashSet<Long> ret = new HashSet<>();
        if (r == null) return ret;
        for (RelationMember m: r.getMembers()) {
            if (m.isWay()) {
                ret.add(m.getMember().getId());
            } else if (m.isRelation() && children) {
                ret.addAll(getWayIdsInRelation(m.getRelation(), true));
            }
        }
        return ret;
    }

    /**
     * Test reading full references for a relation.
     * @throws OsmTransferException if an error occurs
     */
    @Test
    public void testBackreferenceForRelationFull() throws OsmTransferException {
        Relation r = lookupRelation(ds, 1);
        assertNotNull(r);
        // way with name "relation-1" is referred to by four relations:
        //    relation-6, relation-7, relation-8, relation-9
        //

        OsmServerBackreferenceReader reader = new OsmServerBackreferenceReader(r);
        reader.setReadFull(true);
        DataSet referers = reader.parseOsm(NullProgressMonitor.INSTANCE);

        r = lookupRelation(referers, 6);
        assertNotNull(r);
        assertFalse(r.isIncomplete());
        r = lookupRelation(referers, 7);
        assertNotNull(r);
        assertFalse(r.isIncomplete());
        r = lookupRelation(referers, 8);
        assertNotNull(r);
        assertFalse(r.isIncomplete());
        r = lookupRelation(referers, 9);
        assertNotNull(r);
        assertFalse(r.isIncomplete());

        // all relations are fully loaded
        //
        for (Relation r1 : referers.getRelations()) {
            assertFalse(r1.isIncomplete());
        }

        // make sure we read all ways referred to by parent relations. These
        // ways are completely read after reading the relations
        //
        Set<Long> expectedWayIds = new HashSet<>();
        for (RelationMember m : lookupRelation(ds, 6).getMembers()) {
            if (m.isWay()) {
                expectedWayIds.add(m.getMember().getId());
            }
        }
        for (RelationMember m : lookupRelation(ds, 7).getMembers()) {
            if (m.isWay()) {
                expectedWayIds.add(m.getMember().getId());
            }
        }
        for (RelationMember m : lookupRelation(ds, 8).getMembers()) {
            if (m.isWay()) {
                expectedWayIds.add(m.getMember().getId());
            }
        }
        for (RelationMember m : lookupRelation(ds, 9).getMembers()) {
            if (m.isWay()) {
                expectedWayIds.add(m.getMember().getId());
            }
        }
        for (long id : expectedWayIds) {
            Way w = (Way) referers.getPrimitiveById(id, OsmPrimitiveType.WAY);
            assertNotNull(w);
            assertFalse(w.isIncomplete());
        }

        Set<Long> expectedNodeIds = new HashSet<>();
        for (int i = 6; i < 10; i++) {
            Relation r1 = lookupRelation(ds, i);
            expectedNodeIds.addAll(getNodeIdsInRelation(r1, true));
        }

        assertEquals(expectedNodeIds.size(), referers.getNodes().size());
        for (Node n : referers.getNodes()) {
            assertTrue(expectedNodeIds.contains(n.getId()));
        }
    }
}
