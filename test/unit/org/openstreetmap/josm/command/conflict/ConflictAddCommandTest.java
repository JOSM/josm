// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command.conflict;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * Unit tests of {@link ConflictAddCommand} class.
 */
public class ConflictAddCommandTest {

    /**
     * Unit test of {@code ConflictAddCommand#executeCommand} and {@code ConflictAddCommand#undoCommand} methods.
     */
    @Test
    public void testExecuteUndoCommand() {
        OsmDataLayer layer = new OsmDataLayer(new DataSet(), null, null);
        Conflict<OsmPrimitive> conflict = new Conflict<>(null, null);
        ConflictAddCommand cmd = new ConflictAddCommand(layer, conflict);
        assertTrue(cmd.executeCommand());
        assertFalse(layer.getConflicts().isEmpty());
        assertTrue(layer.getConflicts().hasConflict(conflict));
        cmd.undoCommand();
        assertFalse(layer.getConflicts().hasConflict(conflict));
        assertTrue(layer.getConflicts().isEmpty());
    }
}
