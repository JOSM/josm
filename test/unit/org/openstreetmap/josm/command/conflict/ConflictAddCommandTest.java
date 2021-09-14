// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command.conflict;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.command.CommandTest.CommandTestData;
import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.LayerEnvironment;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests of {@link ConflictAddCommand} class.
 */
@BasicPreferences
@LayerEnvironment
class ConflictAddCommandTest {
    private CommandTestData testData;

    /**
     * Setup test.
     */
    @BeforeEach
    public void setUp() {
        testData = new CommandTestData();
    }

    /**
     * Unit test of {@code ConflictAddCommand#executeCommand} and {@code ConflictAddCommand#undoCommand} methods.
     */
    @Test
    void testExecuteUndoCommand() {
        DataSet ds = testData.layer.getDataSet();
        Conflict<Node> conflict = new Conflict<>(testData.existingNode, testData.existingNode2);
        ConflictAddCommand cmd = new ConflictAddCommand(ds, conflict);
        assertTrue(cmd.executeCommand());
        assertFalse(ds.getConflicts().isEmpty());
        assertTrue(ds.getConflicts().hasConflict(conflict));
        cmd.undoCommand();
        assertFalse(ds.getConflicts().hasConflict(conflict));
        assertTrue(ds.getConflicts().isEmpty());
    }

    /**
     * Unit test of {@code ConflictAddCommand#getDescriptionIcon} method.
     */
    @Test
    void testGetDescriptionIcon() {
        Conflict<Node> conflict = new Conflict<>(testData.existingNode, testData.existingNode2);
        assertNotNull(new ConflictAddCommand(testData.layer.getDataSet(), conflict).getDescriptionIcon());
    }

    /**
     * Unit test of methods {@link ConflictAddCommand#equals} and {@link ConflictAddCommand#hashCode}.
     */
    @Test
    void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(ConflictAddCommand.class).usingGetClass()
            .withPrefabValues(DataSet.class,
                    new DataSet(), new DataSet())
            .withPrefabValues(User.class,
                    User.createOsmUser(1, "foo"), User.createOsmUser(2, "bar"))
            .withPrefabValues(Conflict.class,
                    new Conflict<>(new Node(), new Node()), new Conflict<>(new Way(), new Way()))
            .withPrefabValues(OsmDataLayer.class,
                    new OsmDataLayer(new DataSet(), "1", null), new OsmDataLayer(new DataSet(), "2", null))
            .suppress(Warning.NONFINAL_FIELDS)
            .verify();
    }
}
