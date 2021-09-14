// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.mapmode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;

import javax.swing.JList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.PrimitiveRenderer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.Projection;

/**
 * Unit tests for class {@link DrawAction}.
 */
@Main
@Projection
@Timeout(20)
class DrawActionTest {
    /**
     * Non regression test case for bug #12011.
     * Add a new node in the middle of way then undo. The rendering of the node, selected, must not cause any crash in PrimitiveRenderer.
     * @throws SecurityException see {@link Class#getDeclaredField} for details
     * @throws NoSuchFieldException see {@link Class#getDeclaredField} for details
     * @throws IllegalAccessException see {@link Field#set} for details
     * @throws IllegalArgumentException see {@link Field#set} for details
     */
    @Test
    void testTicket12011() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        DataSet dataSet = new DataSet();
        OsmDataLayer layer = new OsmDataLayer(dataSet, OsmDataLayer.createNewName(), null);
        MainApplication.getLayerManager().addLayer(layer);

        // fix map view position
        MapFrame map = MainApplication.getMap();
        map.mapView.zoomTo(new EastNorth(0, 0), 1);

        Node n1 = new Node(new EastNorth(0, 0));
        Node n2 = new Node(new EastNorth(100, 0));

        dataSet.addPrimitive(n1);
        dataSet.addPrimitive(n2);

        Way w = new Way();
        w.setNodes(Arrays.asList(new Node[] {n1, n2}));
        dataSet.addPrimitive(w);

        assertTrue(map.selectDrawTool(false));

        map.mapModeDraw.mouseReleased(new MouseEvent(
                map,
                MouseEvent.MOUSE_RELEASED,
                2000,
                InputEvent.BUTTON1_DOWN_MASK,
                50, 0,
                2, false, MouseEvent.BUTTON1));

        JList<OsmPrimitive> lstPrimitives = new JList<>();
        PrimitiveRenderer renderer = new PrimitiveRenderer();

        assertEquals(3, w.getNodesCount());
        Collection<Node> sel = dataSet.getSelectedNodes();
        assertEquals(1, sel.size());

        Node n3 = sel.iterator().next();

        assertNotNull(renderer.getListCellRendererComponent(lstPrimitives, n3, 0, false, false));

        UndoRedoHandler.getInstance().undo();

        assertEquals(2, w.getNodesCount());
        assertTrue(dataSet.getSelectedNodes().isEmpty());

        assertNotNull(renderer.getListCellRendererComponent(lstPrimitives, n3, 0, false, false));
    }
}

