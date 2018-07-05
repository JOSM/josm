// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
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
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

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
            List<Node> path = graph.buildSpanningPath();
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
     * Unit test of methods {@link NodePair#equals} and {@link NodePair#hashCode}.
     */
    @Test
    public void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(NodePair.class).usingGetClass()
            .suppress(Warning.ANNOTATION) // FIXME: remove it after https://github.com/jqno/equalsverifier/issues/197 is fixed
            .withPrefabValues(Node.class, new Node(1), new Node(2))
            .verify();
    }
}
