// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.sort;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link WayConnectionTypeCalculator} class.
 */
public class WayConnectionTypeCalculatorTest {

    private RelationSorter sorter = new RelationSorter();
    private WayConnectionTypeCalculator wayConnectionTypeCalculator = new WayConnectionTypeCalculator();
    private DataSet testDataset;

    /**
     * Use Mercator projection
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().projection();

    /**
     * Load the test data set
     * @throws IllegalDataException if an error was found while parsing the data
     * @throws IOException in case of I/O error
     */
    @Before
    public void loadData() throws IllegalDataException, IOException {
        if (testDataset == null) {
            try (InputStream fis = Files.newInputStream(Paths.get("data_nodist/relation_sort.osm"))) {
                testDataset = OsmReader.parseDataSet(fis, NullProgressMonitor.INSTANCE);
            }
        }
    }

    private Relation getRelation(String testType) {
        for (Relation r: testDataset.getRelations()) {
            if (testType.equals(r.get("test")))
                return r;
        }
        return null;
    }

    private String getConnections(List<WayConnectionType> connections) {
        String[] result = new String[connections.size()];
        for (int i = 0; i < result.length; i++) {
            WayConnectionType wc = connections.get(i);

            if (wc.isValid()) {
                StringBuilder sb = new StringBuilder();
                if (wc.isLoop) {
                    sb.append("L");
                }
                if (wc.isOnewayLoopForwardPart) {
                    sb.append("FP");
                }
                if (wc.isOnewayLoopBackwardPart) {
                    sb.append("BP");
                }
                if (wc.isOnewayHead) {
                    sb.append("H");
                }
                if (wc.isOnewayTail) {
                    sb.append("T");
                }

                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append(wc.direction);
                result[i] = sb.toString();

            } else {
                result[i] = "I";
            }

        }
        return Arrays.toString(result);
    }

    @Test
    public void testEmpty() {
        String actual = getConnections(wayConnectionTypeCalculator.updateLinks(new ArrayList<>()));
        Assert.assertEquals("[]", actual);
    }

    @Test
    public void testGeneric() {
        Relation relation = getRelation("generic");
        String actual = getConnections(wayConnectionTypeCalculator.updateLinks(relation.getMembers()));
        Assert.assertEquals("[NONE, NONE, FORWARD, FORWARD, NONE, NONE, NONE, I, I]", actual);
        actual = getConnections(wayConnectionTypeCalculator.updateLinks(sorter.sortMembers(relation.getMembers())));
        Assert.assertEquals("[FORWARD, FORWARD, FORWARD, FORWARD, BACKWARD, BACKWARD, NONE, I, I]", actual);
    }

    @Test
    public void testAssociatedStreet() {
        Relation relation = getRelation("associatedStreet");
        String actual = getConnections(wayConnectionTypeCalculator.updateLinks(relation.getMembers()));
        Assert.assertEquals("[NONE, I, I, I, NONE, I]", actual);
        actual = getConnections(wayConnectionTypeCalculator.updateLinks(sorter.sortMembers(relation.getMembers())));
        Assert.assertEquals("[FORWARD, FORWARD, I, I, I, I]", actual);
    }

    @Test
    public void testLoop() {
        Relation relation = getRelation("loop");
        String actual = getConnections(wayConnectionTypeCalculator.updateLinks(relation.getMembers()));
        Assert.assertEquals("[FPH FORWARD, FP FORWARD, NONE, FPH FORWARD, NONE, FPH FORWARD, NONE]", actual);
        //TODO Sorting doesn't work well in this case
        actual = getConnections(wayConnectionTypeCalculator.updateLinks(sorter.sortMembers(relation.getMembers())));
        Assert.assertEquals("[BACKWARD, BACKWARD, BACKWARD, FPH FORWARD, FPH FORWARD, FPH FORWARD, FPH FORWARD]", actual);
    }

    private void reverseWay(Way way) {
        List<Node> nodes = way.getNodes();
        Collections.reverse(nodes);
        way.removeNodes(new HashSet<>(nodes));
        for (Node node : nodes) {
            way.addNode(node);
        }
    }

    /**
     * Test directional {@link WayConnectionTypeCalculator#computeNextWayConnection}
     */
    @Test
    public void testDirectionsOnewaysOnly() {
        Relation relation = getRelation("direction");

        // Check with only one wrong oneway
        List<WayConnectionType> returned = wayConnectionTypeCalculator.updateLinks(relation.getMembers());
        for (int i = 0; i < 4; i++) {
            Assert.assertTrue(returned.get(i).onewayFollowsPrevious);
            Assert.assertTrue(returned.get(i).onewayFollowsNext);
        }

        Assert.assertTrue(returned.get(4).onewayFollowsPrevious);
        Assert.assertFalse(returned.get(4).onewayFollowsNext);

        Assert.assertFalse(returned.get(5).onewayFollowsPrevious);
        Assert.assertFalse(returned.get(5).onewayFollowsNext);

        Assert.assertFalse(returned.get(6).onewayFollowsPrevious);
        Assert.assertTrue(returned.get(6).onewayFollowsNext);

        // Reverse the last oneway
        OsmPrimitive way7 = relation.getMemberPrimitivesList().get(6);
        if (way7 instanceof Way) {
            Way way = (Way) way7;
            reverseWay(way);
            returned = wayConnectionTypeCalculator.updateLinks(relation.getMembers());
            for (int i = 0; i < 4; i++) {
                Assert.assertTrue(returned.get(i).onewayFollowsPrevious);
                Assert.assertTrue(returned.get(i).onewayFollowsNext);
            }

            Assert.assertTrue(returned.get(4).onewayFollowsPrevious);
            Assert.assertFalse(returned.get(4).onewayFollowsNext);

            Assert.assertFalse(returned.get(5).onewayFollowsPrevious);
            Assert.assertTrue(returned.get(5).onewayFollowsNext);

            Assert.assertTrue(returned.get(6).onewayFollowsPrevious);
            Assert.assertTrue(returned.get(6).onewayFollowsNext);
            reverseWay(way);
        }

        // Reverse the wrong oneway
        OsmPrimitive way6 = relation.getMemberPrimitivesList().get(5);
        if (way6 instanceof Way) {
            Way way = (Way) way6;
            reverseWay(way);
            returned = wayConnectionTypeCalculator.updateLinks(relation.getMembers());
            for (int i = 0; i < 7; i++) {
                Assert.assertTrue(returned.get(i).onewayFollowsPrevious);
                Assert.assertTrue(returned.get(i).onewayFollowsNext);
            }
        }

        // Reverse everything
        for (Way way : relation.getMemberPrimitives(Way.class)) {
            reverseWay(way);
        }
        returned = wayConnectionTypeCalculator.updateLinks(relation.getMembers());
        for (int i = 0; i < 7; i++) {
            Assert.assertTrue(returned.get(i).onewayFollowsPrevious);
            Assert.assertTrue(returned.get(i).onewayFollowsNext);
        }
    }

    /**
     * Test directional {@link WayConnectionTypeCalculator#computeNextWayConnection}
     */
    @Test
    public void testDirectionsOnewayMix() {
        Relation relation = getRelation("direction");

        // Remove the oneway in the wrong direction
        OsmPrimitive osm = relation.getMemberPrimitivesList().get(5);
        osm.remove("oneway");
        List<WayConnectionType> returned = wayConnectionTypeCalculator.updateLinks(relation.getMembers());
        for (WayConnectionType type : returned) {
            Assert.assertTrue(type.onewayFollowsNext);
            Assert.assertTrue(type.onewayFollowsPrevious);
        }

        // Check with a oneway=-1 tag without reversing the way
        osm.put("oneway", "-1");
        returned = wayConnectionTypeCalculator.updateLinks(relation.getMembers());
        for (WayConnectionType type : returned) {
            Assert.assertTrue(type.onewayFollowsNext);
            Assert.assertTrue(type.onewayFollowsPrevious);
        }

        // Check with oneways that converge onto a two-way
        // TODO figure out a way to find this situation?
        osm.remove("oneway");
        OsmPrimitive way7 = relation.getMemberPrimitivesList().get(6);
        way7.put("oneway", "-1");
        returned = wayConnectionTypeCalculator.updateLinks(relation.getMembers());
        for (int i = 0; i < returned.size() - 1; i++) {
            WayConnectionType type = returned.get(i);
            Assert.assertTrue(type.onewayFollowsNext);
            Assert.assertTrue(type.onewayFollowsPrevious);
        }
        Assert.assertTrue(returned.get(6).onewayFollowsNext);
        Assert.assertFalse(returned.get(6).onewayFollowsPrevious);
    }
}
