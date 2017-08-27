// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command.conflict;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests of {@link ConflictAddCommand} class.
 */
public class ConflictAddCommandTest {

    private OsmDataLayer layer;

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().platform();

    /**
     * Setup test.
     */
    @Before
    public void setUp() {
        layer = new OsmDataLayer(new DataSet(), null, null);
        MainApplication.getLayerManager().addLayer(layer);
    }

    /**
     * Cleanup test resources.
     */
    @After
    public void tearDown() {
        MainApplication.getLayerManager().removeLayer(layer);
    }

    /**
     * Unit test of {@code ConflictAddCommand#executeCommand} and {@code ConflictAddCommand#undoCommand} methods.
     */
    @Test
    public void testExecuteUndoCommand() {
        DataSet ds = MainApplication.getLayerManager().getEditDataSet();
        Conflict<Node> conflict = new Conflict<>(new Node(), new Node());
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
    public void testGetDescriptionIcon() {
        OsmDataLayer layer = MainApplication.getLayerManager().getEditLayer();
        Conflict<Node> conflict = new Conflict<>(new Node(), new Node());
        assertNotNull(new ConflictAddCommand(layer, conflict).getDescriptionIcon());
    }

    /**
     * Unit test of methods {@link ConflictAddCommand#equals} and {@link ConflictAddCommand#hashCode}.
     */
    @Test
    public void testEqualsContract() {
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
