// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.mapmode;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.Cursor;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * Unit tests for class {@link SelectAction}.
 */
public class SelectActionTest {

    private class MapViewMock extends MapView {
        private OsmDataLayer layer;
        private DataSet currentDataSet;

        MapViewMock(DataSet dataSet, OsmDataLayer layer) {
            super(null, null);
            this.layer = layer;
            this.currentDataSet = dataSet;
        }

        @Override
        public EastNorth getEastNorth(int x, int y) {
            return new EastNorth(x, y);
        }

        @Override
        public void addMouseListener(MouseListener ml) {}

        @Override
        public void removeMouseListener(MouseListener ml) {}

        @Override
        public void setVirtualNodesEnabled(boolean enabled) {}

        @Override
        public void setNewCursor(Cursor cursor, Object reference) {}

        @Override
        public void setNewCursor(int cursor, Object reference) {}

        @Override
        public boolean isActiveLayerVisible() {
            return true;
        }

        @Override
        public void requestClearRect() {}

        @Override
        public Point2D getPoint2D(EastNorth p) {
            return new Point2D.Double(p.getX(), p.getY());
        }

        @Override
        public void setActiveLayer(Layer layer) {}

        @Override
        public Layer getActiveLayer() {
            return layer;
        }

        @Override
        protected DataSet getCurrentDataSet() {
            return currentDataSet;
        }
    }

    /**
     * Override some configuration variables without change in preferences.xml
     */
    class PreferencesMock extends Preferences {
        @Override
        public synchronized int getInteger(String key, int def) {
            if (key == "edit.initial-move-delay") {
                return 0;
            } else {
                return super.getInteger(key, def);
            }
        }
    }

    boolean nodesMerged;

    class SelectActionMock extends SelectAction {
        SelectActionMock(MapFrame mapFrame, DataSet dataSet, OsmDataLayer layer) {
            super(mapFrame);
            try {
                Field mv = SelectAction.class.getDeclaredField("mv");
                mv.setAccessible(true);
                mv.set(this, new MapViewMock(dataSet, layer));
            } catch (Exception e) {
                e.printStackTrace();
                fail("Can't setup testing environnement");
            }
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
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init(true);
    }

    /**
     * Test case: Move a two nodes way near a third node.
     * Resulting way should be attach to the third node.
     * see #10748
     */
    @Test
    public void test10748() {
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

        Main.pref = new PreferencesMock();
        Main.main.addLayer(layer);
        SelectAction action = new SelectActionMock(Main.map, dataSet, layer);
        nodesMerged = false;

        action.setEnabled(true);
        action.putValue("active", true);

        MouseEvent event;
        event = new MouseEvent(Main.map,
                               MouseEvent.MOUSE_PRESSED,
                               0,
                               InputEvent.BUTTON1_MASK | InputEvent.CTRL_MASK,
                               100, 0,
                               1,
                               false);
        action.mousePressed(event);
        event = new MouseEvent(Main.map,
                               MouseEvent.MOUSE_DRAGGED,
                               1000,
                               InputEvent.BUTTON1_MASK | InputEvent.CTRL_MASK,
                               50, 0,
                               1,
                               false);
        action.mouseDragged(event);
        event = new MouseEvent(Main.map,
                               MouseEvent.MOUSE_RELEASED,
                               2000,
                               InputEvent.BUTTON1_MASK | InputEvent.CTRL_MASK,
                               5, 0,
                               1,
                               false);
        action.mouseReleased(event);

        // As result of test, we must find a 2 nodes way, from EN(0, 0) to EN(100, 0)
        assertTrue("Nodes are not merged", nodesMerged);
        assertSame(String.format("Expect exactly one way, found %d\n", dataSet.getWays().size()),
                   dataSet.getWays().size(), 1);
        Way rw = dataSet.getWays().iterator().next();
        assertFalse("Way shouldn't be deleted\n", rw.isDeleted());
        assertSame(String.format("Way shouldn't have 2 nodes, %d found\n", w.getNodesCount()),
                   rw.getNodesCount(), 2);
        Node r1 = rw.firstNode();
        Node r2 = rw.lastNode();
        if (r1.getEastNorth().east() > r2.getEastNorth().east()) {
            Node tmp = r1;
            r1 = r2;
            r2 = tmp;
        }
        assertSame(String.format("East should be 0, found %f\n", r1.getEastNorth().east()),
                   Double.compare(r1.getEastNorth().east(), 0), 0);
        assertSame(String.format("East should be 100, found %f\n", r2.getEastNorth().east()),
                   Double.compare(r2.getEastNorth().east(), 100), 0);
    }
}
