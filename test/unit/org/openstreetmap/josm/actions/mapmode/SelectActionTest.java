// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.mapmode;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.actions.mapmode.SelectAction.Mode;
import org.openstreetmap.josm.actions.mapmode.SelectAction.SelectActionCursor;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.layer.MainLayerManager;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.Projection;
import org.openstreetmap.josm.tools.ReflectionUtils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link SelectAction}.
 */
@Main
@Projection
class SelectActionTest {

    boolean nodesMerged;

    class SelectActionMock extends SelectAction {
        SelectActionMock(MapFrame mapFrame, DataSet dataSet, OsmDataLayer layer) throws ReflectiveOperationException {
            super(mapFrame);
            Field mv = SelectAction.class.getDeclaredField("mv");
            ReflectionUtils.setObjectsAccessible(mv);
            mv.set(this, new MapViewMock(new MainLayerManager()));
        }

        @Override
        public void mergeNodes(OsmDataLayer layer, Collection<Node> nodes,
                               Node targetLocationNode) {
            assertSame(2, nodes.size(), String.format("Should merge two nodes, %d found", nodes.size()));
            nodesMerged = true;
        }
    }

    /**
     * Test case: Move a two nodes way near a third node.
     * Resulting way should be attach to the third node.
     * see #10748
     * @throws ReflectiveOperationException if an error occurs
     */
    @Test
    @SuppressFBWarnings(value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    void testTicket10748() throws ReflectiveOperationException {
        DataSet dataSet = new DataSet();
        OsmDataLayer layer = new OsmDataLayer(dataSet, OsmDataLayer.createNewName(), null);

        Node n1 = new Node(new EastNorth(0, 0));
        Node n2 = new Node(new EastNorth(100, 0));
        Node n3 = new Node(new EastNorth(200, 0));
        dataSet.addPrimitive(n1);
        dataSet.addPrimitive(n2);
        dataSet.addPrimitive(n3);

        Way w = new Way();
        w.setNodes(Arrays.asList(new Node[] {n2, n3}));
        dataSet.addPrimitive(w);

        dataSet.addSelected(n2);
        dataSet.addSelected(w);

        Config.getPref().put("edit.initial-move-delay", "0");
        MainApplication.getLayerManager().addLayer(layer);
        MapFrame map = MainApplication.getMap();
        SelectAction action = new SelectActionMock(map, dataSet, layer);
        nodesMerged = false;

        action.setEnabled(true);
        action.putValue("active", true);

        MouseEvent event;
        event = new MouseEvent(map,
                               MouseEvent.MOUSE_PRESSED,
                               0,
                               InputEvent.BUTTON1_DOWN_MASK | InputEvent.CTRL_DOWN_MASK,
                               100, 0,
                               1,
                               false, MouseEvent.BUTTON1);
        action.mousePressed(event);
        event = new MouseEvent(map,
                               MouseEvent.MOUSE_DRAGGED,
                               1000,
                               InputEvent.BUTTON1_DOWN_MASK | InputEvent.CTRL_DOWN_MASK,
                               50, 0,
                               1,
                               false, MouseEvent.BUTTON1);
        action.mouseDragged(event);
        event = new MouseEvent(map,
                               MouseEvent.MOUSE_RELEASED,
                               2000,
                               InputEvent.BUTTON1_DOWN_MASK | InputEvent.CTRL_DOWN_MASK,
                               5, 0,
                               1,
                               false, MouseEvent.BUTTON1);
        action.mouseReleased(event);

        // As result of test, we must find a 2 nodes way, from EN(0, 0) to EN(100, 0)
        assertTrue(nodesMerged, "Nodes are not merged");
        assertSame(1, dataSet.getWays().size(), String.format("Expect exactly one way, found %d%n", dataSet.getWays().size()));
        Way rw = dataSet.getWays().iterator().next();
        assertFalse(rw.isDeleted(), "Way shouldn't be deleted\n");
        assertSame(2, rw.getNodesCount(), String.format("Way shouldn't have 2 nodes, %d found%n", w.getNodesCount()));
        Node r1 = rw.firstNode();
        Node r2 = rw.lastNode();
        if (r1.getEastNorth().east() > r2.getEastNorth().east()) {
            Node tmp = r1;
            r1 = r2;
            r2 = tmp;
        }
        assertSame(0, Double.compare(r1.getEastNorth().east(), 0),
                String.format("East should be 0, found %f%n", r1.getEastNorth().east()));
        assertSame(0, Double.compare(r2.getEastNorth().east(), 100),
                String.format("East should be 100, found %f%n", r2.getEastNorth().east()));
    }

    /**
     * Unit test of {@link Mode} enum.
     */
    @Test
    void testEnumMode() {
        TestUtils.superficialEnumCodeCoverage(Mode.class);
    }

    /**
     * Unit test of {@link SelectActionCursor} enum.
     */
    @Test
    void testEnumSelectActionCursor() {
        TestUtils.superficialEnumCodeCoverage(SelectActionCursor.class);
    }
}

