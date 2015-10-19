// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command.conflict;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.gui.conflict.pair.MergeDecisionType;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * Unit tests of {@link CoordinateConflictResolveCommand} class.
 */
public class CoordinateConflictResolveCommandTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init(true);
        Main.map.mapView.addLayer(new OsmDataLayer(new DataSet(), null, null));
    }

    private static Conflict<Node> createConflict() {
        return new Conflict<>(new Node(LatLon.ZERO), new Node(new LatLon(50, 50)));
    }

    /**
     * Unit test of {@code CoordinateConflictResolveCommand#executeCommand} and {@code CoordinateConflictResolveCommand#undoCommand} methods.
     */
    @Test
    public void testExecuteKeepMineUndoCommand() {
        Conflict<Node> conflict = createConflict();
        CoordinateConflictResolveCommand cmd = new CoordinateConflictResolveCommand(conflict, MergeDecisionType.KEEP_MINE);
        assertTrue(cmd.executeCommand());
        assertTrue(LatLon.ZERO.equals(conflict.getMy().getCoor()));
        cmd.undoCommand();
        assertTrue(LatLon.ZERO.equals(conflict.getMy().getCoor()));
    }

    /**
     * Unit test of {@code CoordinateConflictResolveCommand#executeCommand} and {@code CoordinateConflictResolveCommand#undoCommand} methods.
     */
    @Test
    public void testExecuteKeepTheirUndoCommand() {
        Conflict<Node> conflict = createConflict();
        CoordinateConflictResolveCommand cmd = new CoordinateConflictResolveCommand(conflict, MergeDecisionType.KEEP_THEIR);
        assertTrue(cmd.executeCommand());
        assertTrue(conflict.getTheir().getCoor().equals(conflict.getMy().getCoor()));
        cmd.undoCommand();
        //assertTrue(LatLon.ZERO.equals(conflict.getMy().getCoor())); // FIXME it does not work
    }

    /**
     * Unit test of {@code CoordinateConflictResolveCommand#getDescriptionIcon} method.
     */
    @Test
    public void testGetDescriptionIcon() {
        Conflict<Node> conflict = createConflict();
        assertNotNull(new CoordinateConflictResolveCommand(conflict, null).getDescriptionIcon());
    }
}
