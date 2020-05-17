// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.NodePair;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import nl.jqno.equalsverifier.EqualsVerifier;

/**
 * Unit tests for class {@link CombineWayAction}.
 */
public class CombineWayActionTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Non-regression test for bug #11957.
     * @throws IOException if any I/O error occurs
     * @throws IllegalDataException if OSM parsing fails
     */
    @Test
    public void testTicket11957() throws IOException, IllegalDataException {
        try (InputStream is = TestUtils.getRegressionDataStream(11957, "data.osm")) {
            DataSet ds = OsmReader.parseDataSet(is, null);
            List<Node> path = CombineWayAction.tryJoin(ds.getWays());
            assertEquals(10, path.size());
            Set<Long> firstAndLastObtained = new HashSet<>();
            firstAndLastObtained.add(path.get(0).getId());
            firstAndLastObtained.add(path.get(path.size()-1).getId());
            Set<Long> firstAndLastExpected = new HashSet<>();
            firstAndLastExpected.add(1618969016L);
            firstAndLastExpected.add(35213705L);
            assertEquals(firstAndLastExpected, firstAndLastObtained);
        }
    }

    /**
     * Non-regression test for bug #18385 (combine way with overlapping ways)
     * @throws IOException if any I/O error occurs
     * @throws IllegalDataException if OSM parsing fails
     */
    @Test
    public void testTicket18385() throws IOException, IllegalDataException {
        try (InputStream is = TestUtils.getRegressionDataStream(18385, "data.osm")) {
            DataSet ds = OsmReader.parseDataSet(is, null);
            List<Node> path = CombineWayAction.tryJoin(ds.getWays());
            assertFalse(path.isEmpty());
        }
    }

    /**
     * Non-regression test for bug #18387 (combine new way with oneway)
     * @throws IOException if any I/O error occurs
     * @throws IllegalDataException if OSM parsing fails
     */
    @Test
    public void testTicket18387() throws IOException, IllegalDataException {
        try (InputStream is = TestUtils.getRegressionDataStream(18387, "data.osm")) {
            DataSet ds = OsmReader.parseDataSet(is, null);
            ArrayList<Way> selection = new ArrayList<>(ds.getWays());
            assertEquals(2, selection.size());
            if (!selection.get(0).isNew())
                Collections.reverse(selection);
            double expectedLen = getOriginalLength(selection);
            List<Node> path = CombineWayAction.tryJoin(selection);
            assertFalse(path.isEmpty());
            Way combined = new Way(0);
            combined.setNodes(path);
            assertEquals(expectedLen, combined.getLength(), 0.01);
        }
    }

    /**
     * Non-regression test for bug #18367 (Lines cannot be combined when they share an overlapping segment)
     * @throws IOException if any I/O error occurs
     * @throws IllegalDataException if OSM parsing fails
     */
    @Test
    public void testTicket18367() throws IOException, IllegalDataException {
        try (InputStream is = TestUtils.getRegressionDataStream(18367, "nocombine.osm")) {
            DataSet ds = OsmReader.parseDataSet(is, null);
            ArrayList<Way> selection = new ArrayList<>(ds.getWays());
            assertEquals(2, selection.size());
            double expectedLen = getOriginalLength(selection);
            List<Node> path = CombineWayAction.tryJoin(selection);
            assertFalse(path.isEmpty());
            Way combined = new Way(0);
            combined.setNodes(path);
            assertEquals(expectedLen, combined.getLength(), 1e-7);
        }
    }


    /**
     * Non-regression test for bug #18367
     * @throws IOException if any I/O error occurs
     * @throws IllegalDataException if OSM parsing fails
     */
    @Test
    public void testTicket18367NeedsSplit() throws IOException, IllegalDataException {
        try (InputStream is = TestUtils.getRegressionDataStream(18367, "split-and-reverse.osm")) {
            DataSet ds = OsmReader.parseDataSet(is, null);
            ArrayList<Way> selection = new ArrayList<>(ds.getWays());
            double expectedLen = getOriginalLength(selection);
            List<Node> path = CombineWayAction.tryJoin(selection);
            assertFalse(path.isEmpty());
            Way combined = new Way(0);
            combined.setNodes(path);
            assertEquals(expectedLen, combined.getLength(), 1e-7);
            List<Way> reversedWays = new LinkedList<>();
            List<Way> unreversedWays = new LinkedList<>();
            CombineWayAction.detectReversedWays(selection, path, reversedWays, unreversedWays);
            assertFalse(reversedWays.isEmpty());
        }
    }


    /**
     * Test for bad reverse way detection. See #18367
     * @throws IOException if any I/O error occurs
     * @throws IllegalDataException if OSM parsing fails
     */
    @Test
    public void testDetectReversedWays() throws IOException, IllegalDataException {
        try (InputStream is = TestUtils.getRegressionDataStream(18367, "silent-revert.osm")) {
            DataSet ds = OsmReader.parseDataSet(is, null);
            ArrayList<Way> selection = new ArrayList<>(ds.getWays());
            assertEquals(2, selection.size());
            // make sure that short way is first
            if (selection.get(0).getNodesCount() != 2)
                Collections.reverse(selection);
            double expectedLen = getOriginalLength(selection);
            List<Node> path = CombineWayAction.tryJoin(selection);
            assertFalse(path.isEmpty());
            Way combined = new Way(0);
            combined.setNodes(path);
            assertEquals(expectedLen, combined.getLength(), 1e-7);
            List<Way> reversedWays = new LinkedList<>();
            List<Way> unreversedWays = new LinkedList<>();
            CombineWayAction.detectReversedWays(selection, path, reversedWays, unreversedWays);
            assertFalse(reversedWays.contains(selection.get(0)));
        }
    }


    /**
     * Unit test of methods {@link NodePair#equals} and {@link NodePair#hashCode}.
     */
    @Test
    public void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(NodePair.class).usingGetClass()
            .withPrefabValues(Node.class, new Node(1), new Node(2))
            .verify();
    }

    private static double getOriginalLength(Collection<Way> ways) {
        return ways.stream().mapToDouble(Way::getLength).sum();
    }

}
