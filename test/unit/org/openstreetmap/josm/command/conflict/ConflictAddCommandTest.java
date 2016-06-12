// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command.conflict;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests of {@link ConflictAddCommand} class.
 */
public class ConflictAddCommandTest {

    private static OsmDataLayer layer;

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init(true);
        layer = new OsmDataLayer(new DataSet(), null, null);
        Main.main.addLayer(layer);
    }

    /**
     * Cleanup test resources.
     */
    @AfterClass
    public static void tearDownAfterClass() {
        Main.main.removeLayer(layer);
    }

    /**
     * Unit test of {@code ConflictAddCommand#executeCommand} and {@code ConflictAddCommand#undoCommand} methods.
     */
    @Test
    public void testExecuteUndoCommand() {
        OsmDataLayer layer = Main.getLayerManager().getEditLayer();
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
        OsmDataLayer layer = Main.getLayerManager().getEditLayer();
        Conflict<Node> conflict = new Conflict<>(new Node(), new Node());
        assertNotNull(new ConflictAddCommand(layer, conflict).getDescriptionIcon());
    }

    /**
     * Unit test of methods {@link ConflictAddCommand#equals} and {@link ConflictAddCommand#hashCode}.
     */
    @Test
    public void equalsContract() {
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
