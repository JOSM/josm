// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.mapmode;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.Main;
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
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.Utils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link SelectAction}.
 */
public class SelectActionTest {

    boolean nodesMerged;

    class SelectActionMock extends SelectAction {
        SelectActionMock(MapFrame mapFrame, DataSet dataSet, OsmDataLayer layer) throws ReflectiveOperationException {
            super(mapFrame);
            Field mv = SelectAction.class.getDeclaredField("mv");
            Utils.setObjectsAccessible(mv);
            mv.set(this, new MapViewMock(new MainLayerManager()));
        }

        @Override
        public void mergeNodes(OsmDataLayer layer, Collection<Node> nodes,
                               Node targetLocationNode) {
            assertSame(String.format("Should merge two nodes, %d found", nodes.size()),
                       nodes.size(), 2);
            nodesMerged = true;
        }
    }

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().platform().projection().main();

    /**
     * Test case: Move a two nodes way near a third node.
     * Resulting way should be attach to the third node.
     * see #10748
     * @throws ReflectiveOperationException if an error occurs
     */
    @Test
    @SuppressFBWarnings(value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    public void testTicket10748() throws ReflectiveOperationException {
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

        Main.pref.put("edit.initial-move-delay", "0");
        MainApplication.getLayerManager().addLayer(layer);
        try {
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
            assertTrue("Nodes are not merged", nodesMerged);
            assertSame(String.format("Expect exactly one way, found %d%n", dataSet.getWays().size()),
                       dataSet.getWays().size(), 1);
            Way rw = dataSet.getWays().iterator().next();
            assertFalse("Way shouldn't be deleted\n", rw.isDeleted());
            assertSame(String.format("Way shouldn't have 2 nodes, %d found%n", w.getNodesCount()),
                       rw.getNodesCount(), 2);
            Node r1 = rw.firstNode();
            Node r2 = rw.lastNode();
            if (r1.getEastNorth().east() > r2.getEastNorth().east()) {
                Node tmp = r1;
                r1 = r2;
                r2 = tmp;
            }
            assertSame(String.format("East should be 0, found %f%n", r1.getEastNorth().east()),
                       Double.compare(r1.getEastNorth().east(), 0), 0);
            assertSame(String.format("East should be 100, found %f%n", r2.getEastNorth().east()),
                       Double.compare(r2.getEastNorth().east(), 100), 0);
        } finally {
            // Ensure we clean the place before leaving, even if test fails.
            MainApplication.getLayerManager().removeLayer(layer);
        }
    }

    /**
     * Unit test of {@link Mode} enum.
     */
    @Test
    public void testEnumMode() {
        TestUtils.superficialEnumCodeCoverage(Mode.class);
    }

    /**
     * Unit test of {@link SelectActionCursor} enum.
     */
    @Test
    public void testEnumSelectActionCursor() {
        TestUtils.superficialEnumCodeCoverage(SelectActionCursor.class);
    }
}
