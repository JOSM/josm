// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.awt.geom.Area;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.GeoPropertyIndex;
import org.openstreetmap.josm.tools.GeoPropertyIndex.GeoProperty;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.RightAndLefthandTraffic;

/**
 * Unit tests for class {@link CreateCircleAction}.
 */
public final class CreateCircleActionTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init(true);
    }

    /**
     * FIXME: Conveniance method to prevent Selection Change events.
     * A more proper way should be to define a TestingDataSet class with limited
     * functionalities, but DataSet is declare as final (due to Cloneable interface).
     *
     * I don't know why, but in other tests there are no problem to add selected primitives
     * but in this case there is a problem with an even listener of selection change.
     */
    public void addSelected(OsmPrimitive p, DataSet ds) {
        try {
            Method method = ds.getClass()
                .getDeclaredMethod("addSelected",
                                   new Class<?>[] {Collection.class, boolean.class});
            method.setAccessible(true);
            method.invoke(ds, Collections.singleton(p), false);
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue("Can't add OsmPrimitive to dataset", false);
        }
    }

    /**
     * Test case: When Create Circle action is performed with a single way selected,
     * circle direction must equals way direction.
     * see #7421
     */
    @Test
    public void test7421_0() {
        DataSet dataSet = new DataSet();
        OsmDataLayer layer = new OsmDataLayer(dataSet, OsmDataLayer.createNewName(), null);

        Node n1 = new Node(new EastNorth(0, 0));
        Node n2 = new Node(new EastNorth(-1, 1));
        Node n3 = new Node(new EastNorth(1, 1));
        dataSet.addPrimitive(n1);
        dataSet.addPrimitive(n2);
        dataSet.addPrimitive(n3);

        Way w = new Way(); // Way is Clockwize
        w.setNodes(Arrays.asList(new Node[] {n1, n2, n3}));
        dataSet.addPrimitive(w);

        addSelected(w, dataSet);

        CreateCircleAction action = new CreateCircleAction();
        action.setEnabled(true);
        try {
            Main.main.addLayer(layer);
            action.actionPerformed(null);
        } finally {
            // Ensure we clean the place before leaving, even if test fails.
            Main.map.mapView.removeLayer(layer);
        }

        // Expected result: Dataset contain one closed way, clockwise
        Collection<Way> resultingWays = dataSet.getWays();
        assertSame(String.format("Expect one way after perform action. %d found", resultingWays.size()),
                   resultingWays.size(), 1);
        Way resultingWay = resultingWays.iterator().next();
        assertTrue("Resulting way is not closed",
                   resultingWay.isClosed());
        assertTrue("Found anti-clockwize circle while way was clockwize",
                   Geometry.isClockwise(resultingWay));
    }

    /**
     * Mock left/right hand traffic database with constant traffic hand
     */
    private static class ConstantTrafficHand implements GeoProperty<Boolean> {
        boolean isLeft;

        ConstantTrafficHand(boolean isLeft) {
            this.isLeft = isLeft;
        }

        @Override
        public Boolean get(LatLon ll) {
            return isLeft;
        }

        @Override
        public Boolean get(BBox box) {
            return isLeft;
        }
    }

    /**
     * Test case: When Create Circle action is performed with nodes, resulting
     * circle direction depend on traffic hand. Simulate a left hand traffic.
     * see #7421
     */
    @Test
    public void test7421_1() {
        DataSet dataSet = new DataSet();
        OsmDataLayer layer = new OsmDataLayer(dataSet, OsmDataLayer.createNewName(), null);

        Node n1 = new Node(new EastNorth(0, 0));
        Node n2 = new Node(new EastNorth(-1, 1));
        Node n3 = new Node(new EastNorth(1, 1));
        dataSet.addPrimitive(n1);
        dataSet.addPrimitive(n2);
        dataSet.addPrimitive(n3);

        addSelected(n1, dataSet);
        addSelected(n2, dataSet);
        addSelected(n3, dataSet);

        // Mock left/right hand traffic database
        try {
            Field leftHandTrafficPolygons = RightAndLefthandTraffic.class
                .getDeclaredField("leftHandTrafficPolygons");
            leftHandTrafficPolygons.setAccessible(true);
            leftHandTrafficPolygons.set(null, new ArrayList<Area>());
            Field rlCache = RightAndLefthandTraffic.class.getDeclaredField("rlCache");
            rlCache.setAccessible(true);
            ConstantTrafficHand trafficHand = new ConstantTrafficHand(true);
            rlCache.set(null, new GeoPropertyIndex<Boolean>(trafficHand, 24));
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue("Impossible to mock left/right hand database", false);
        }

        CreateCircleAction action = new CreateCircleAction();
        action.setEnabled(true);
        try {
            Main.main.addLayer(layer);
            action.actionPerformed(null);
        } finally {
            // Ensure we clean the place before leaving, even if test fails.
            Main.map.mapView.removeLayer(layer);
        }

        // Expected result: Dataset contain one closed way, clockwise
        Collection<Way> resultingWays = dataSet.getWays();
        assertSame(String.format("Expect one way after perform action. %d found", resultingWays.size()),
                   resultingWays.size(), 1);
        Way resultingWay = resultingWays.iterator().next();
        assertTrue("Resulting way is not closed",
                   resultingWay.isClosed());
        assertTrue("Found anti-clockwise way while traffic is left hand.",
                   Geometry.isClockwise(resultingWay));
    }
}
