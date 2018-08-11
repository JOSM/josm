// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.command.CommandTest.CommandTestDataWithRelation;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests of {@link MoveCommand} class.
 */
public class MoveCommandTest {
    /**
     * We need prefs for nodes.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().i18n().projection();
    private CommandTestDataWithRelation testData;

    /**
     * Set up the test data.
     */
    @Before
    public void createTestData() {
        testData = new CommandTestDataWithRelation();
    }

    /**
     * Test the various constructors.
     */
    @Test
    public void testConstructors() {
        EastNorth offset = new EastNorth(1, 2);
        LatLon destLatLon = ProjectionRegistry.getProjection().eastNorth2latlon(offset);
        EastNorth start = new EastNorth(2, 0);

        Set<OsmPrimitive> nodeAsCollection = Collections.<OsmPrimitive>singleton(testData.existingNode);
        assertEquals(1, nodeAsCollection.size());
        checkCommandAfterConstructor(new MoveCommand(nodeAsCollection, offset));
        checkCommandAfterConstructor(new MoveCommand(testData.existingNode, destLatLon));
        checkCommandAfterConstructor(new MoveCommand(nodeAsCollection, 1, 2));
        checkCommandAfterConstructor(new MoveCommand(nodeAsCollection, start, start.add(offset)));
        checkCommandAfterConstructor(new MoveCommand(testData.existingNode, 1, 2));
        checkCommandAfterConstructor(new MoveCommand(testData.existingNode, start, start.add(offset)));
    }

    private void checkCommandAfterConstructor(MoveCommand moveCommand) {
        ArrayList<OsmPrimitive> nodes = new ArrayList<>();
        moveCommand.fillModifiedData(nodes, null, null);
        assertEquals(nodes, new ArrayList<>(Collections.<OsmPrimitive>singleton(testData.existingNode)));

        assertEquals("east", 1, moveCommand.getOffset().east(), 0.0001);
        assertEquals("north", 2, moveCommand.getOffset().north(), 0.0001);
    }

    /**
     * Test {@link MoveCommand#executeCommand()} for simple nodes.
     */
    @Test
    public void testSingleMove() {
        MoveCommand command = new MoveCommand(testData.existingNode, 1, 2);
        testData.existingNode.setEastNorth(new EastNorth(3, 7));
        command.executeCommand();
        assertEquals("east", 4, testData.existingNode.getEastNorth().east(), 0.0001);
        assertEquals("north", 9, testData.existingNode.getEastNorth().north(), 0.0001);
    }

    /**
     * Test {@link MoveCommand#executeCommand()} for multiple nodes.
     */
    @Test
    public void testMultipleMove() {
        MoveCommand command = new MoveCommand(
                Arrays.asList(testData.existingNode, testData.existingNode2, testData.existingWay),
                new EastNorth(1, 2));

        testData.existingNode.setEastNorth(new EastNorth(3, 7));
        testData.existingNode2.setEastNorth(new EastNorth(4, 7));
        command.executeCommand();

        assertEquals("east", 4, testData.existingNode.getEastNorth().east(), 0.0001);
        assertEquals("north", 9, testData.existingNode.getEastNorth().north(), 0.0001);
        assertEquals("east", 5, testData.existingNode2.getEastNorth().east(), 0.0001);
        assertEquals("north", 9, testData.existingNode2.getEastNorth().north(), 0.0001);
    }

    /**
     * Test {@link MoveCommand#moveAgain(double, double)} and {@link MoveCommand#moveAgainTo(double, double)}.
     */
    @Test
    public void testMoveAgain() {
        MoveCommand command = new MoveCommand(testData.existingNode, 1, 2);
        assertEquals("east", 1, command.getOffset().east(), 0.0001);
        assertEquals("north", 2, command.getOffset().north(), 0.0001);

        command.moveAgain(1, 2);
        assertEquals("east", 2, command.getOffset().east(), 0.0001);
        assertEquals("north", 4, command.getOffset().north(), 0.0001);

        command.moveAgain(-9, -3);
        assertEquals("east", -7, command.getOffset().east(), 0.0001);
        assertEquals("north", 1, command.getOffset().north(), 0.0001);

        command.moveAgainTo(1, 2);
        assertEquals("east", 1, command.getOffset().east(), 0.0001);
        assertEquals("north", 2, command.getOffset().north(), 0.0001);
    }

    /**
     * Test {@link MoveCommand#saveCheckpoint()} and {@link MoveCommand#resetToCheckpoint()}
     */
    @Test
    public void testCheckpoint() {
        MoveCommand command = new MoveCommand(testData.existingNode, 2, 4);
        assertEquals("east", 2, command.getOffset().east(), 0.0001);
        assertEquals("north", 4, command.getOffset().north(), 0.0001);

        command.saveCheckpoint();
        command.moveAgain(3, 7);
        assertEquals("east", 5, command.getOffset().east(), 0.0001);
        assertEquals("north", 11, command.getOffset().north(), 0.0001);

        command.resetToCheckpoint();
        assertEquals("east", 2, command.getOffset().east(), 0.0001);
        assertEquals("north", 4, command.getOffset().north(), 0.0001);
    }

    /**
     * Test the start point mechanism.
     */
    @Test
    public void testStartPoint() {
        EastNorth start = new EastNorth(10, 20);
        MoveCommand command = new MoveCommand(testData.existingNode, start, start.add(1, 2));
        assertEquals("east", 1, command.getOffset().east(), 0.0001);
        assertEquals("north", 2, command.getOffset().north(), 0.0001);

        command.applyVectorTo(start.add(3, 4));
        assertEquals("east", 3, command.getOffset().east(), 0.0001);
        assertEquals("north", 4, command.getOffset().north(), 0.0001);

        // set to 100, 200
        command.changeStartPoint(new EastNorth(103, 204));
        command.applyVectorTo(new EastNorth(101, 202));
        assertEquals("east", 1, command.getOffset().east(), 0.0001);
        assertEquals("north", 2, command.getOffset().north(), 0.0001);
    }

    /**
     * Test the start point mechanism ignored.
     */
    @Test
    public void testNoStartPoint() {
        MoveCommand command = new MoveCommand(testData.existingNode, 1, 0);
        // ignored
        command.applyVectorTo(new EastNorth(3, 4));
        assertEquals("east", 1, command.getOffset().east(), 0.0001);
        assertEquals("north", 0, command.getOffset().north(), 0.0001);

        // set to 100, 200
        command.changeStartPoint(new EastNorth(101, 200));
        // works
        command.applyVectorTo(new EastNorth(101, 202));
        assertEquals("east", 1, command.getOffset().east(), 0.0001);
        assertEquals("north", 2, command.getOffset().north(), 0.0001);
    }

    /**
     * Test {@link MoveCommand#undoCommand()}
     */
    @Test
    public void testUndo() {
        testData.existingNode.setEastNorth(new EastNorth(3, 7));
        MoveCommand command = new MoveCommand(testData.existingNode, 1, 2);
        command.executeCommand();
        assertEquals("east", 4, testData.existingNode.getEastNorth().east(), 0.0001);
        assertEquals("north", 9, testData.existingNode.getEastNorth().north(), 0.0001);

        command.undoCommand();
        assertEquals("east", 3, testData.existingNode.getEastNorth().east(), 0.0001);
        assertEquals("north", 7, testData.existingNode.getEastNorth().north(), 0.0001);

        command.executeCommand();
        assertEquals("east", 4, testData.existingNode.getEastNorth().east(), 0.0001);
        assertEquals("north", 9, testData.existingNode.getEastNorth().north(), 0.0001);
    }

    /**
     * Tests {@link MoveCommand#fillModifiedData(java.util.Collection, java.util.Collection, java.util.Collection)}
     */
    @Test
    public void testFillModifiedData() {
        ArrayList<OsmPrimitive> modified = new ArrayList<>();
        ArrayList<OsmPrimitive> deleted = new ArrayList<>();
        ArrayList<OsmPrimitive> added = new ArrayList<>();
        new MoveCommand(Arrays.<OsmPrimitive>asList(testData.existingNode), 1, 2).fillModifiedData(modified,
                deleted, added);
        assertArrayEquals(new Object[] {testData.existingNode }, modified.toArray());
        assertArrayEquals(new Object[] {}, deleted.toArray());
        assertArrayEquals(new Object[] {}, added.toArray());
    }

    /**
     * Tests {@link MoveCommand#getParticipatingPrimitives()}
     */
    @Test
    public void testGetParticipatingPrimitives() {
        MoveCommand command = new MoveCommand(Arrays.<OsmPrimitive>asList(testData.existingNode), 1, 2);
        command.executeCommand();
        assertArrayEquals(new Object[] {testData.existingNode}, command.getParticipatingPrimitives().toArray());

        MoveCommand command2 = new MoveCommand(
                Arrays.<OsmPrimitive>asList(testData.existingNode, testData.existingWay), 1, 2);
        command2.executeCommand();
        assertArrayEquals(new Object[] {testData.existingNode, testData.existingNode2},
                command2.getParticipatingPrimitives().toArray());
    }

    /**
     * Test {@link MoveCommand#getDescriptionText()}
     */
    @Test
    public void testDescription() {
        Node node = TestUtils.addFakeDataSet(new Node(LatLon.ZERO));
        node.put("name", "xy");
        List<OsmPrimitive> nodeList = Arrays.<OsmPrimitive>asList(node);
        assertTrue(new MoveCommand(nodeList, 1, 2).getDescriptionText().matches("Move 1 node"));
        List<OsmPrimitive> nodes = Arrays.<OsmPrimitive>asList(node, testData.existingNode, testData.existingNode2);
        assertTrue(new MoveCommand(nodes, 1, 2).getDescriptionText().matches("Move 3 nodes"));
    }

    /**
     * Unit test of methods {@link MoveCommand#equals} and {@link MoveCommand#hashCode}.
     */
    @Test
    public void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(MoveCommand.class).usingGetClass()
            .withPrefabValues(LatLon.class,
                LatLon.ZERO, new LatLon(45, 45))
            .withPrefabValues(DataSet.class,
                new DataSet(), new DataSet())
            .withPrefabValues(User.class,
                    User.createOsmUser(1, "foo"), User.createOsmUser(2, "bar"))
            .withPrefabValues(OsmDataLayer.class,
                new OsmDataLayer(new DataSet(), "1", null), new OsmDataLayer(new DataSet(), "2", null))
            .suppress(Warning.NONFINAL_FIELDS)
            .verify();
    }
}
