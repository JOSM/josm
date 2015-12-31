// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command.conflict;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * Unit tests of {@link ConflictAddCommand} class.
 */
public class ConflictAddCommandTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init(true);
        Main.map.mapView.addLayer(new OsmDataLayer(new DataSet(), null, null));
    }

    /**
     * Unit test of {@code ConflictAddCommand#executeCommand} and {@code ConflictAddCommand#undoCommand} methods.
     */
    @Test
    public void testExecuteUndoCommand() {
        OsmDataLayer layer = Main.map.mapView.getEditLayer();
        Conflict<Node> conflict = new Conflict<>(new Node(), new Node());
        ConflictAddCommand cmd = new ConflictAddCommand(layer, conflict);
        assertTrue(cmd.executeCommand());
        assertFalse(layer.getConflicts().isEmpty());
        assertTrue(layer.getConflicts().hasConflict(conflict));
        cmd.undoCommand();
        assertFalse(layer.getConflicts().hasConflict(conflict));
        assertTrue(layer.getConflicts().isEmpty());
    }

    /**
     * Unit test of {@code ConflictAddCommand#getDescriptionIcon} method.
     */
    @Test
    public void testGetDescriptionIcon() {
        OsmDataLayer layer = Main.map.mapView.getEditLayer();
        Conflict<Node> conflict = new Conflict<>(new Node(), new Node());
        assertNotNull(new ConflictAddCommand(layer, conflict).getDescriptionIcon());
    }

    /**
     * Unit test of methods {@link ConflictAddCommand#equals} and {@link ConflictAddCommand#hashCode}.
     */
    @Test
    public void equalsContract() {
        EqualsVerifier.forClass(ConflictAddCommand.class).usingGetClass()
            .withPrefabValues(Conflict.class,
                    new Conflict<>(new Node(), new Node()), new Conflict<>(new Way(), new Way()))
            .withPrefabValues(OsmDataLayer.class,
                    new OsmDataLayer(new DataSet(), "1", null), new OsmDataLayer(new DataSet(), "2", null))
            .suppress(Warning.NONFINAL_FIELDS)
            .verify();
    }
}
