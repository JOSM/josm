// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.NodeGraph;
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
            NodeGraph graph = NodeGraph.createNearlyUndirectedGraphFromNodeWays(ds.getWays());
            List<Node> path = graph.buildSpanningPathNoRemove();
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
            NodeGraph graph = NodeGraph.createNearlyUndirectedGraphFromNodeWays(ds.getWays());
            List<Node> path = graph.buildSpanningPathNoRemove();
            assertNull(path);
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
            NodeGraph graph = NodeGraph.createNearlyUndirectedGraphFromNodeWays(selection);
            List<Node> path = graph.buildSpanningPathNoRemove();
            assertTrue(path != null);
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
}
