// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.I18n;
import org.openstreetmap.josm.testutils.annotations.Projection;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests of {@link MoveCommand} class.
 */
@BasicPreferences
@I18n
@Projection
class MoveCommandTest {
    private CommandTestDataWithRelation testData;

    /**
     * Set up the test data.
     */
    @BeforeEach
    public void createTestData() {
        testData = new CommandTestDataWithRelation();
    }

    /**
     * Test the various constructors.
     */
    @Test
    void testConstructors() {
        EastNorth offset = new EastNorth(1, 2);
        LatLon destLatLon = ProjectionRegistry.getProjection().eastNorth2latlon(offset);
        EastNorth start = new EastNorth(2, 0);

        Set<OsmPrimitive> nodeAsCollection = Collections.singleton(testData.existingNode);
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

        assertEquals(1, moveCommand.getOffset().east(), 0.0001, "east");
        assertEquals(2, moveCommand.getOffset().north(), 0.0001, "north");
        assertEquals(2.236068, moveCommand.getDistance(n -> true), 0.0001, "distance");
    }

    /**
     * Test {@link MoveCommand#executeCommand()} for simple nodes.
     */
    @Test
    void testSingleMove() {
        MoveCommand command = new MoveCommand(testData.existingNode, 1, 2);
        testData.existingNode.setEastNorth(new EastNorth(3, 7));
        command.executeCommand();
        assertEquals(4, testData.existingNode.getEastNorth().east(), 0.0001, "east");
        assertEquals(9, testData.existingNode.getEastNorth().north(), 0.0001, "north");
        assertEquals(2.236068, command.getDistance(n -> true), 0.0001, "distance");
    }

    /**
     * Test {@link MoveCommand#executeCommand()} for multiple nodes.
     */
    @Test
    void testMultipleMove() {
        MoveCommand command = new MoveCommand(
                Arrays.asList(testData.existingNode, testData.existingNode2, testData.existingWay),
                new EastNorth(1, 2));

        testData.existingNode.setEastNorth(new EastNorth(3, 7));
        testData.existingNode2.setEastNorth(new EastNorth(4, 7));
        command.executeCommand();

        assertEquals(4, testData.existingNode.getEastNorth().east(), 0.0001, "east");
        assertEquals(9, testData.existingNode.getEastNorth().north(), 0.0001, "north");
        assertEquals(5, testData.existingNode2.getEastNorth().east(), 0.0001, "east");
        assertEquals(9, testData.existingNode2.getEastNorth().north(), 0.0001, "north");
    }

    /**
     * Test {@link MoveCommand#moveAgain(double, double)} and {@link MoveCommand#moveAgainTo(double, double)}.
     */
    @Test
    void testMoveAgain() {
        MoveCommand command = new MoveCommand(testData.existingNode, 1, 2);
        assertEquals(1, command.getOffset().east(), 0.0001, "east");
        assertEquals(2, command.getOffset().north(), 0.0001, "north");

        command.moveAgain(1, 2);
        assertEquals(2, command.getOffset().east(), 0.0001, "east");
        assertEquals(4, command.getOffset().north(), 0.0001, "north");

        command.moveAgain(-9, -3);
        assertEquals(-7, command.getOffset().east(), 0.0001, "east");
        assertEquals(1, command.getOffset().north(), 0.0001, "north");

        command.moveAgainTo(1, 2);
        assertEquals(1, command.getOffset().east(), 0.0001, "east");
        assertEquals(2, command.getOffset().north(), 0.0001, "north");
    }

    /**
     * Test {@link MoveCommand#saveCheckpoint()} and {@link MoveCommand#resetToCheckpoint()}
     */
    @Test
    void testCheckpoint() {
        MoveCommand command = new MoveCommand(testData.existingNode, 2, 4);
        assertEquals(2, command.getOffset().east(), 0.0001, "east");
        assertEquals(4, command.getOffset().north(), 0.0001, "north");

        command.saveCheckpoint();
        command.moveAgain(3, 7);
        assertEquals(5, command.getOffset().east(), 0.0001, "east");
        assertEquals(11, command.getOffset().north(), 0.0001, "north");

        command.resetToCheckpoint();
        assertEquals(2, command.getOffset().east(), 0.0001, "east");
        assertEquals(4, command.getOffset().north(), 0.0001, "north");
    }

    /**
     * Test the start point mechanism.
     */
    @Test
    void testStartPoint() {
        EastNorth start = new EastNorth(10, 20);
        MoveCommand command = new MoveCommand(testData.existingNode, start, start.add(1, 2));
        assertEquals(1, command.getOffset().east(), 0.0001, "east");
        assertEquals(2, command.getOffset().north(), 0.0001, "north");

        command.applyVectorTo(start.add(3, 4));
        assertEquals(3, command.getOffset().east(), 0.0001, "east");
        assertEquals(4, command.getOffset().north(), 0.0001, "north");

        // set to 100, 200
        command.changeStartPoint(new EastNorth(103, 204));
        command.applyVectorTo(new EastNorth(101, 202));
        assertEquals(1, command.getOffset().east(), 0.0001, "east");
        assertEquals(2, command.getOffset().north(), 0.0001, "north");
    }

    /**
     * Test the start point mechanism ignored.
     */
    @Test
    void testNoStartPoint() {
        MoveCommand command = new MoveCommand(testData.existingNode, 1, 0);
        // ignored
        command.applyVectorTo(new EastNorth(3, 4));
        assertEquals(1, command.getOffset().east(), 0.0001, "east");
        assertEquals(0, command.getOffset().north(), 0.0001, "north");

        // set to 100, 200
        command.changeStartPoint(new EastNorth(101, 200));
        // works
        command.applyVectorTo(new EastNorth(101, 202));
        assertEquals(1, command.getOffset().east(), 0.0001, "east");
        assertEquals(2, command.getOffset().north(), 0.0001, "north");
    }

    /**
     * Test {@link MoveCommand#undoCommand()}
     */
    @Test
    void testUndo() {
        testData.existingNode.setEastNorth(new EastNorth(3, 7));
        MoveCommand command = new MoveCommand(testData.existingNode, 1, 2);
        command.executeCommand();
        assertEquals(4, testData.existingNode.getEastNorth().east(), 0.0001, "east");
        assertEquals(9, testData.existingNode.getEastNorth().north(), 0.0001, "north");

        command.undoCommand();
        assertEquals(3, testData.existingNode.getEastNorth().east(), 0.0001, "east");
        assertEquals(7, testData.existingNode.getEastNorth().north(), 0.0001, "north");

        command.executeCommand();
        assertEquals(4, testData.existingNode.getEastNorth().east(), 0.0001, "east");
        assertEquals(9, testData.existingNode.getEastNorth().north(), 0.0001, "north");
    }

    /**
     * Tests {@link MoveCommand#fillModifiedData(java.util.Collection, java.util.Collection, java.util.Collection)}
     */
    @Test
    void testFillModifiedData() {
        ArrayList<OsmPrimitive> modified = new ArrayList<>();
        ArrayList<OsmPrimitive> deleted = new ArrayList<>();
        ArrayList<OsmPrimitive> added = new ArrayList<>();
        new MoveCommand(Collections.singletonList(testData.existingNode), 1, 2).fillModifiedData(modified,
                deleted, added);
        assertArrayEquals(new Object[] {testData.existingNode }, modified.toArray());
        assertArrayEquals(new Object[] {}, deleted.toArray());
        assertArrayEquals(new Object[] {}, added.toArray());
    }

    /**
     * Tests {@link MoveCommand#getParticipatingPrimitives()}
     */
    @Test
    void testGetParticipatingPrimitives() {
        MoveCommand command = new MoveCommand(Collections.singletonList(testData.existingNode), 1, 2);
        command.executeCommand();
        assertArrayEquals(new Object[] {testData.existingNode}, command.getParticipatingPrimitives().toArray());

        MoveCommand command2 = new MoveCommand(
                Arrays.asList(testData.existingNode, testData.existingWay), 1, 2);
        command2.executeCommand();
        assertArrayEquals(new Object[] {testData.existingNode, testData.existingNode2},
                command2.getParticipatingPrimitives().toArray());
    }

    /**
     * Test {@link MoveCommand#getDescriptionText()}
     */
    @Test
    void testDescription() {
        Node node = TestUtils.addFakeDataSet(new Node(LatLon.ZERO));
        node.put("name", "xy");
        List<OsmPrimitive> nodeList = Collections.singletonList(node);
        assertTrue(new MoveCommand(nodeList, 1, 2).getDescriptionText().matches("Move 1 node"));
        List<OsmPrimitive> nodes = Arrays.asList(node, testData.existingNode, testData.existingNode2);
        assertTrue(new MoveCommand(nodes, 1, 2).getDescriptionText().matches("Move 3 nodes"));
    }

    /**
     * Unit test of methods {@link MoveCommand#equals} and {@link MoveCommand#hashCode}.
     */
    @Test
    void testEqualsContract() {
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
