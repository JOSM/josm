// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command.conflict;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.command.CommandTest.CommandTestData;
import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.conflict.pair.MergeDecisionType;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.LayerEnvironment;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests of {@link CoordinateConflictResolveCommand} class.
 */
@BasicPreferences
@LayerEnvironment
class CoordinateConflictResolveCommandTest {

    private CommandTestData testData;

    /**
     * Setup test.
     */
    @BeforeEach
    public void setUp() {
        testData = new CommandTestData();
    }

    private Conflict<Node> createConflict() {
        return new Conflict<>(testData.existingNode, testData.existingNode2);
    }

    /**
     * Unit test of {@code CoordinateConflictResolveCommand#executeCommand} and {@code CoordinateConflictResolveCommand#undoCommand} methods.
     */
    @Test
    void testExecuteKeepMineUndoCommand() {
        Conflict<Node> conflict = createConflict();
        CoordinateConflictResolveCommand cmd = new CoordinateConflictResolveCommand(conflict, MergeDecisionType.KEEP_MINE);
        assertTrue(cmd.executeCommand());
        assertEquals(LatLon.ZERO, conflict.getMy().getCoor());
        cmd.undoCommand();
        assertEquals(LatLon.ZERO, conflict.getMy().getCoor());
    }

    /**
     * Unit test of {@code CoordinateConflictResolveCommand#executeCommand} and {@code CoordinateConflictResolveCommand#undoCommand} methods.
     */
    @Test
    void testExecuteKeepTheirUndoCommand() {
        Conflict<Node> conflict = createConflict();
        CoordinateConflictResolveCommand cmd = new CoordinateConflictResolveCommand(conflict, MergeDecisionType.KEEP_THEIR);
        assertTrue(cmd.executeCommand());
        assertEquals(conflict.getTheir().getCoor(), conflict.getMy().getCoor());
        cmd.undoCommand();
        //assertEquals(LatLon.ZERO, conflict.getMy().getCoor()); // FIXME it does not work
    }

    /**
     * Unit test of {@code CoordinateConflictResolveCommand#getDescriptionIcon} method.
     */
    @Test
    void testGetDescriptionIcon() {
        Conflict<Node> conflict = createConflict();
        assertNotNull(new CoordinateConflictResolveCommand(conflict, null).getDescriptionIcon());
    }

    /**
     * Unit test of methods {@link CoordinateConflictResolveCommand#equals} and {@link CoordinateConflictResolveCommand#hashCode}.
     */
    @Test
    void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(CoordinateConflictResolveCommand.class).usingGetClass()
            .withPrefabValues(Conflict.class,
                    new Conflict<>(new Node(), new Node()), new Conflict<>(new Way(), new Way()))
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
