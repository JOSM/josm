// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.mapmode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;

import javax.swing.JList;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.PrimitiveRenderer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link DrawAction}.
 */
public class DrawActionTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().platform().main().projection().timeout(20000);

    /**
     * Non regression test case for bug #12011.
     * Add a new node in the middle of way then undo. The rendering of the node, selected, must not cause any crash in PrimitiveRenderer.
     * @throws SecurityException see {@link Class#getDeclaredField} for details
     * @throws NoSuchFieldException see {@link Class#getDeclaredField} for details
     * @throws IllegalAccessException see {@link Field#set} for details
     * @throws IllegalArgumentException see {@link Field#set} for details
     */
    @Test
    public void testTicket12011() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
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

        try {
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

            MainApplication.undoRedo.undo();

            assertEquals(2, w.getNodesCount());
            assertTrue(dataSet.getSelectedNodes().isEmpty());

            assertNotNull(renderer.getListCellRendererComponent(lstPrimitives, n3, 0, false, false));
        } finally {
            // Ensure we clean the place before leaving, even if test fails.
            MainApplication.getLayerManager().removeLayer(layer);
        }
    }

    /**
     * Non regression test case for bug #14762.
     * @deprecated test to remove
     */
    @Test
    @Deprecated
    public void testTicket14762() {
        DrawAction action = new DrawAction();
        assertNull(action.getLayerManager().getEditDataSet());
        assertEquals(0, action.getPreservedPrimitives().size());
    }
}
