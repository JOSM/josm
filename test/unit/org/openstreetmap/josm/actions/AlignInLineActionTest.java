// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.actions.AlignInLineAction.InvalidSelection;
import org.openstreetmap.josm.actions.AlignInLineAction.Line;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link AlignInLineAction}.
 */
public final class AlignInLineActionTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().main().projection();

    /** Class under test. */
    private static AlignInLineAction action;

    /**
     * Setup test.
     */
    @Before
    public void setUp() {
        // Enable "Align in line" feature.
        action = MainApplication.getMenu().alignInLine;
        action.setEnabled(true);
    }

    /**
     * Test case: only nodes selected, part of an open way: align these nodes on the line passing through the extremity
     * nodes (the most distant in the way sequence, not the most euclidean-distant). See
     * https://josm.openstreetmap.de/ticket/9605#comment:3. Note that in this test, after alignment, way is overlapping
     * itself.
     * @throws InvalidSelection never
     */
    @Test
    public void testNodesOpenWay() throws InvalidSelection {
        DataSet dataSet = new DataSet();
        OsmDataLayer layer = new OsmDataLayer(dataSet, OsmDataLayer.createNewName(), null);

        // Create test points, lower left is (0,0).
        //
        // 1 - - -
        // - 3 - 2
        // - - - -
        Node point1 = new Node(new EastNorth(0, 2));
        Node point2 = new Node(new EastNorth(3, 1));
        Node point3 = new Node(new EastNorth(1, 1));

        try {
            MainApplication.getLayerManager().addLayer(layer);

            // Create an open way.
            createWay(dataSet, point1, point2, point3);

            // Select nodes to align.
            dataSet.addSelected(point1, point2, point3);

            action.buildCommand().executeCommand();
        } finally {
            // Ensure we clean the place before leaving, even if test fails.
            MainApplication.getLayerManager().removeLayer(layer);
        }

        // Points 1 and 3 are the extremities and must not have moved. Only point 2 must have moved.
        assertCoordEq(point1, 0, 2);
        assertCoordEq(point2, 2, 0);
        assertCoordEq(point3, 1, 1);
    }

    /**
     * Test case: only nodes selected, part of a closed way: align these nodes on the line passing through the most
     * distant nodes.
     * @throws InvalidSelection never
     */
    @Test
    public void testNodesClosedWay() throws InvalidSelection {
        DataSet dataSet = new DataSet();
        OsmDataLayer layer = new OsmDataLayer(dataSet, OsmDataLayer.createNewName(), null);

        // Create test points, lower left is (0,0).
        //
        // 4 - 3
        // - - -
        // 1 - 2
        Node point1 = new Node(new EastNorth(0, 0));
        Node point2 = new Node(new EastNorth(2, 0));
        Node point3 = new Node(new EastNorth(2, 2));
        Node point4 = new Node(new EastNorth(0, 2));

        try {
            MainApplication.getLayerManager().addLayer(layer);

            // Create a closed way.
            createWay(dataSet, point1, point2, point3, point4, point1);
            // Select nodes to align (point1 must be in the second position to exhibit the bug).
            dataSet.addSelected(point4, point1, point2);

            action.buildCommand().executeCommand();
        } finally {
            // Ensure we clean the place before leaving, even if test fails.
            MainApplication.getLayerManager().removeLayer(layer);
        }

        // Only point 1 must have moved.
        assertCoordEq(point1, 1, 1);
        assertCoordEq(point2, 2, 0);
        assertCoordEq(point3, 2, 2);
        assertCoordEq(point4, 0, 2);
    }

    /**
     * Test case: only nodes selected, part of multiple ways: align these nodes on the line passing through the most
     * distant nodes.
     * @throws InvalidSelection never
     */
    @Test
    public void testNodesOpenWays() throws InvalidSelection {
        DataSet dataSet = new DataSet();
        OsmDataLayer layer = new OsmDataLayer(dataSet, OsmDataLayer.createNewName(), null);

        // Create test points, lower left is (0,0).
        //
        // 1 - -
        // 3 - 2
        // - - 4
        Node point1 = new Node(new EastNorth(0, 2));
        Node point2 = new Node(new EastNorth(2, 1));
        Node point3 = new Node(new EastNorth(0, 1));
        Node point4 = new Node(new EastNorth(2, 0));

        try {
            MainApplication.getLayerManager().addLayer(layer);

            // Create 2 ways.
            createWay(dataSet, point1, point2);
            createWay(dataSet, point3, point4);

            // Select nodes to align.
            dataSet.addSelected(point1, point2, point3, point4);

            // Points must align between points 1 and 4.
            action.buildCommand().executeCommand();
        } finally {
            // Ensure we clean the place before leaving, even if test fails.
            MainApplication.getLayerManager().removeLayer(layer);
        }

        assertCoordEq(point1, 0, 2);
        assertCoordEq(point2, 1.5, 0.5);
        assertCoordEq(point3, 0.5, 1.5);
        assertCoordEq(point4, 2, 0);
    }

    /**
     * Create a way made of the provided nodes and select nodes.
     *
     * @param dataSet Dataset in which adding nodes.
     * @param nodes List of nodes to add to dataset.
     */
    private void createWay(DataSet dataSet, Node... nodes) {
        Way way = new Way();
        dataSet.addPrimitive(way);

        for (Node node : nodes) {
            // Add primitive to dataset only if not already included.
            if (dataSet.getPrimitiveById(node) == null)
                dataSet.addPrimitive(node);

            way.addNode(node);
        }
    }

    /**
     * Assert that the provided node has the specified coordinates. If not fail the test.
     *
     * @param node Node to test.
     * @param x X coordinate.
     * @param y Y coordinate.
     */
    private void assertCoordEq(Node node, double x, double y) {
        EastNorth coordinate = node.getEastNorth();
        assertEquals("Wrong x coordinate.", x, coordinate.getX(), LatLon.MAX_SERVER_PRECISION);
        assertEquals("Wrong y coordinate.", y, coordinate.getY(), LatLon.MAX_SERVER_PRECISION);
    }

    /**
     * Test that a {@link Line} can be constructed with nodes of different coordinates.
     * @throws InvalidSelection never
     */
    @Test
    public void testLineDifferentCoordinates() throws InvalidSelection {
        assertNotNull(new Line(new Node(new EastNorth(0, 1)),
                               new Node(new EastNorth(0, 2))));
        assertNotNull(new Line(new Node(new EastNorth(0, 1)),
                               new Node(new EastNorth(1, 1))));
        assertNotNull(new Line(new Node(new EastNorth(0, 1)),
                               new Node(new EastNorth(0+1e-150, 1+1e-150))));
    }

    /**
     * Test that a {@link Line} cannot be constructed with nodes of same coordinates.
     * @throws InvalidSelection always
     */
    @Test(expected = InvalidSelection.class)
    public void testLineSameCoordinates1() throws InvalidSelection {
        new Line(new Node(new EastNorth(0, 1)),
                 new Node(new EastNorth(0, 1)));
    }

    /**
     * Test that a {@link Line} cannot be constructed with nodes of same coordinates.
     * @throws InvalidSelection always
     */
    @Test(expected = InvalidSelection.class)
    public void testLineSameCoordinates2() throws InvalidSelection {
        new Line(new Node(new EastNorth(0, 1)),
                 new Node(new EastNorth(0+1e-175, 1+1e-175)));
    }
}
