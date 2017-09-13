// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link MultiFetchServerObjectReader}.
 */
@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS")
public class MultiFetchServerObjectReaderTest {
    private static Logger logger = Logger.getLogger(MultiFetchServerObjectReader.class.getName());

    /**
     * Global timeout applied to all test methods.
     */
    @Rule
    public Timeout globalTimeout = Timeout.seconds(60);

    /**
     * builds a large data set to be used later for testing MULTI FETCH on the server
     *
     * @return a large data set
     */
    protected static DataSet buildTestDataSet() {
        DataSet ds = new DataSet();
        ds.setVersion("0.6");
        Random rand = new SecureRandom();

        int numNodes = 1000;
        int numWays = 1000;
        int numRelations = 1000;

        ArrayList<Node> nodes = new ArrayList<>();
        ArrayList<Way> ways = new ArrayList<>();

        // create a set of nodes
        //
        for (int i = 0; i < numNodes; i++) {
            Node n = new Node();
            n.setCoor(new LatLon(-36.6, 47.6));
            n.put("name", "node-"+i);
            ds.addPrimitive(n);
            nodes.add(n);
        }

        // create a set of ways, each with a random number of nodes
        //
        for (int i = 0; i < numWays; i++) {
            Way w = new Way();
            int numNodesInWay = 2 + (int) Math.round(rand.nextDouble() * 5);
            int start = (int) Math.round(rand.nextDouble() * numNodes);
            for (int j = 0; j < numNodesInWay; j++) {
                int idx = (start + j) % numNodes;
                Node n = nodes.get(idx);
                w.addNode(n);
            }
            w.put("name", "way-"+i);
            ds.addPrimitive(w);
            ways.add(w);
        }

        // create a set of relations each with a random number of nodes, and ways
        //
        for (int i = 0; i < numRelations; i++) {
            Relation r = new Relation();
            r.put("name", "relation-" +i);
            int numNodesInRelation = (int) Math.round(rand.nextDouble() * 10);
            int start = (int) Math.round(rand.nextDouble() * numNodes);
            for (int j = 0; j < numNodesInRelation; j++) {
                int idx = (start + j) % 500;
                Node n = nodes.get(idx);
                r.addMember(new RelationMember("role-" + j, n));
            }
            int numWaysInRelation = (int) Math.round(rand.nextDouble() * 10);
            start = (int) Math.round(rand.nextDouble() * numWays);
            for (int j = 0; j < numWaysInRelation; j++) {
                int idx = (start + j) % 500;
                Way w = ways.get(idx);
                r.addMember(new RelationMember("role-" + j, w));
            }
            ds.addPrimitive(r);
        }

        return ds;
    }

    private static DataSet testDataSet;

    /**
     * creates the dataset on the server.
     *
     * @param ds the data set
     * @throws OsmTransferException if something goes wrong
     */
    public static void createDataSetOnServer(DataSet ds) throws OsmTransferException {
        logger.info("creating data set on the server ...");
        ArrayList<OsmPrimitive> primitives = new ArrayList<>();
        primitives.addAll(testDataSet.getNodes());
        primitives.addAll(testDataSet.getWays());
        primitives.addAll(testDataSet.getRelations());

        OsmServerWriter writer = new OsmServerWriter();
        Changeset cs = new Changeset();
        writer.uploadOsm(new UploadStrategySpecification().setStrategy(UploadStrategy.SINGLE_REQUEST_STRATEGY),
                primitives, cs, NullProgressMonitor.INSTANCE);
        OsmApi.getOsmApi().closeChangeset(cs, NullProgressMonitor.INSTANCE);
    }

    /**
     * Setup test.
     * @throws Exception if an error occurs
     */
    @BeforeClass
    public static void init() throws Exception {
        logger.info("initializing ...");
        JOSMFixture.createFunctionalTestFixture().init();

        Main.pref.put("osm-server.auth-method", "basic");

        // don't use atomic upload, the test API server can't cope with large diff uploads
        Main.pref.putBoolean("osm-server.atomic-upload", false);

        File dataSetCacheOutputFile = new File(System.getProperty("java.io.tmpdir"),
                MultiFetchServerObjectReaderTest.class.getName() + ".dataset");

        String p = System.getProperties().getProperty("useCachedDataset");
        if (p != null && Boolean.parseBoolean(p.trim().toLowerCase(Locale.ENGLISH))) {
            logger.info(MessageFormat.format("property ''{0}'' set, using cached dataset", "useCachedDataset"));
            return;
        }

        logger.info(MessageFormat.format(
                "property ''{0}'' not set to true, creating test dataset on the server. property is ''{1}''", "useCachedDataset", p));

        // build and upload the test data set
        logger.info("creating test data set ....");
        testDataSet = buildTestDataSet();
        logger.info("uploading test data set ...");
        createDataSetOnServer(testDataSet);

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
     * Test to multi-get 10 nodes.
     * @throws OsmTransferException if an error occurs
     */
    @Test
    public void testMultiGet10Nodes() throws OsmTransferException {
        MultiFetchServerObjectReader reader = new MultiFetchServerObjectReader();
        ArrayList<Node> nodes = new ArrayList<>(ds.getNodes());
        for (int i = 0; i < 10; i++) {
            reader.append(nodes.get(i));
        }
        DataSet out = reader.parseOsm(NullProgressMonitor.INSTANCE);
        assertEquals(10, out.getNodes().size());
        for (Node n1:out.getNodes()) {
            Node n2 = (Node) ds.getPrimitiveById(n1);
            assertNotNull(n2);
            assertEquals(n2.get("name"), n2.get("name"));
        }
        assertTrue(reader.getMissingPrimitives().isEmpty());
    }

    /**
     * Test to multi-get 10 ways.
     * @throws OsmTransferException if an error occurs
     */
    @Test
    public void testMultiGet10Ways() throws OsmTransferException {
        MultiFetchServerObjectReader reader = new MultiFetchServerObjectReader();
        ArrayList<Way> ways = new ArrayList<>(ds.getWays());
        for (int i = 0; i < 10; i++) {
            reader.append(ways.get(i));
        }
        DataSet out = reader.parseOsm(NullProgressMonitor.INSTANCE);
        assertEquals(10, out.getWays().size());
        for (Way w1: out.getWays()) {
            Way w2 = (Way) ds.getPrimitiveById(w1);
            assertNotNull(w2);
            assertEquals(w2.getNodesCount(), w1.getNodesCount());
            assertEquals(w2.get("name"), w1.get("name"));
        }
        assertTrue(reader.getMissingPrimitives().isEmpty());
    }

    /**
     * Test to multi-get 10 relations.
     * @throws OsmTransferException if an error occurs
     */
    @Test
    public void testMultiGet10Relations() throws OsmTransferException {
        MultiFetchServerObjectReader reader = new MultiFetchServerObjectReader();
        ArrayList<Relation> relations = new ArrayList<>(ds.getRelations());
        for (int i = 0; i < 10; i++) {
            reader.append(relations.get(i));
        }
        DataSet out = reader.parseOsm(NullProgressMonitor.INSTANCE);
        assertEquals(10, out.getRelations().size());
        for (Relation r1: out.getRelations()) {
            Relation r2 = (Relation) ds.getPrimitiveById(r1);
            assertNotNull(r2);
            assertEquals(r2.getMembersCount(), r1.getMembersCount());
            assertEquals(r2.get("name"), r2.get("name"));
        }
        assertTrue(reader.getMissingPrimitives().isEmpty());
    }

    /**
     * Test to multi-get 800 nodes.
     * @throws OsmTransferException if an error occurs
     */
    @Test
    public void testMultiGet800Nodes() throws OsmTransferException {
        MultiFetchServerObjectReader reader = new MultiFetchServerObjectReader();
        ArrayList<Node> nodes = new ArrayList<>(ds.getNodes());
        for (int i = 0; i < 812; i++) {
            reader.append(nodes.get(i));
        }
        DataSet out = reader.parseOsm(NullProgressMonitor.INSTANCE);
        assertEquals(812, out.getNodes().size());
        for (Node n1:out.getNodes()) {
            Node n2 = (Node) ds.getPrimitiveById(n1);
            assertNotNull(n2);
            assertEquals(n2.get("name"), n2.get("name"));
        }
        assertTrue(reader.getMissingPrimitives().isEmpty());
    }

    /**
     * Test to multi-get non-existing node.
     * @throws OsmTransferException if an error occurs
     */
    @Test
    public void testMultiGetWithNonExistingNode() throws OsmTransferException {
        MultiFetchServerObjectReader reader = new MultiFetchServerObjectReader();
        ArrayList<Node> nodes = new ArrayList<>(ds.getNodes());
        for (int i = 0; i < 10; i++) {
            reader.append(nodes.get(i));
        }
        Node n = new Node(9999999);
        reader.append(n); // doesn't exist
        DataSet out = reader.parseOsm(NullProgressMonitor.INSTANCE);
        assertEquals(10, out.getNodes().size());
        for (Node n1:out.getNodes()) {
            Node n2 = (Node) ds.getPrimitiveById(n1);
            assertNotNull(n2);
            assertEquals(n2.get("name"), n2.get("name"));
        }
        assertFalse(reader.getMissingPrimitives().isEmpty());
        assertEquals(1, reader.getMissingPrimitives().size());
        assertEquals(9999999, reader.getMissingPrimitives().iterator().next().getUniqueId());
    }
}
