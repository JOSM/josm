// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.command.CommandTest.CommandTestData;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests of {@link RotateCommand} class.
 */
public class RotateCommandTest {

    /**
     * We need prefs for nodes.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().projection();
    private CommandTestData testData;

    /**
     * Set up the test data.
     */
    @Before
    public void createTestData() {
        testData = new CommandTestData();
    }

    /**
     * Test a simple 45Â° rotation. Tests {@link RotateCommand#executeCommand()}
     */
    @Test
    public void testRotate() {
        // pivot needs to be at 0,0
        Node n1 = new Node(new EastNorth(10, 10));
        Node n2 = new Node(new EastNorth(-1, 0));
        Node n3 = new Node(new EastNorth(-9, -10));
        new DataSet(n1, n2, n3);
        RotateCommand rotate = new RotateCommand(Arrays.asList(n1, n2, n3), new EastNorth(0, 0));
        rotate.setRotationAngle(Math.PI / 4);
        rotate.executeCommand();

        assertEquals(Math.sqrt(2) * 10, n1.getEastNorth().east(), 0.0001);
        assertEquals(0, n1.getEastNorth().north(), 0.0001);
        assertEquals(-1 / Math.sqrt(2), n2.getEastNorth().east(), 0.0001);
        assertEquals(1 / Math.sqrt(2), n2.getEastNorth().north(), 0.0001);
    }

    /**
     * Test {@link RotateCommand#undoCommand()}
     */
    @Test
    public void testUndo() {
        Node n1 = new Node(new EastNorth(10, 10));
        Node n2 = new Node(new EastNorth(-1, 0));
        Node n3 = new Node(new EastNorth(-9, -10));
        new DataSet(n1, n2, n3);
        RotateCommand rotate = new RotateCommand(Arrays.asList(n1, n2, n3), new EastNorth(0, 0));
        rotate.setRotationAngle(Math.PI / 4);
        rotate.executeCommand();
        rotate.undoCommand();

        assertEquals(10, n1.getEastNorth().east(), 0.0001);
        assertEquals(10, n1.getEastNorth().north(), 0.0001);
        assertEquals(-1, n2.getEastNorth().east(), 0.0001);
        assertEquals(0, n2.getEastNorth().north(), 0.0001);

        rotate.executeCommand();

        assertEquals(-1 / Math.sqrt(2), n2.getEastNorth().east(), 0.0001);
        assertEquals(1 / Math.sqrt(2), n2.getEastNorth().north(), 0.0001);
    }

    /**
     * Tests {@link RotateCommand#fillModifiedData(java.util.Collection, java.util.Collection, java.util.Collection)}
     */
    @Test
    public void testFillModifiedData() {
        ArrayList<OsmPrimitive> modified = new ArrayList<>();
        ArrayList<OsmPrimitive> deleted = new ArrayList<>();
        ArrayList<OsmPrimitive> added = new ArrayList<>();
        RotateCommand command = new RotateCommand(Arrays.asList(testData.existingNode),
                new EastNorth(0, 0));
        // intentionally empty
        command.fillModifiedData(modified, deleted, added);
        assertArrayEquals(new Object[] {}, modified.toArray());
        assertArrayEquals(new Object[] {}, deleted.toArray());
        assertArrayEquals(new Object[] {}, added.toArray());
    }

    /**
     * Tests {@link RotateCommand#getParticipatingPrimitives()}
     */
    @Test
    public void testGetParticipatingPrimitives() {
        RotateCommand command = new RotateCommand(Arrays.asList(testData.existingNode), new EastNorth(0, 0));
        command.executeCommand();
        assertArrayEquals(new Object[] {testData.existingNode}, command.getParticipatingPrimitives().toArray());
    }

    /**
     * Test {@link RotateCommand#getDescriptionText()}
     */
    @Test
    public void testDescription() {
        assertEquals("Rotate 1 node",
                new RotateCommand(Arrays.asList(testData.existingNode), new EastNorth(0, 0))
                        .getDescriptionText());
        assertEquals("Rotate 2 nodes",
                new RotateCommand(Arrays.asList(testData.existingNode, testData.existingNode2), new EastNorth(0, 0))
                        .getDescriptionText());
    }

    /**
     * Unit test of methods {@link RotateCommand#equals} and {@link RotateCommand#hashCode}.
     */
    @Test
    public void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(RotateCommand.class).usingGetClass()
                .withPrefabValues(LatLon.class, LatLon.ZERO, new LatLon(45, 45))
                .withPrefabValues(DataSet.class, new DataSet(), new DataSet())
                .withPrefabValues(User.class, User.createOsmUser(1, "foo"), User.createOsmUser(2, "bar"))
                .withPrefabValues(OsmDataLayer.class, new OsmDataLayer(new DataSet(), "1", null),
                        new OsmDataLayer(new DataSet(), "2", null))
                .suppress(Warning.NONFINAL_FIELDS).verify();
    }
}
