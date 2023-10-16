// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.testutils.annotations.Projection;
import org.openstreetmap.josm.tools.GeoProperty;
import org.openstreetmap.josm.tools.GeoPropertyIndex;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.ReflectionUtils;
import org.openstreetmap.josm.tools.RightAndLefthandTraffic;

/**
 * Unit tests for class {@link CreateCircleAction}.
 */
@Projection
final class CreateCircleActionTest {
    /**
     * Test case: When Create Circle action is performed with a single way selected,
     * circle direction must equals way direction.
     * see #7421
     */
    @Test
    void testTicket7421case0() {
        DataSet dataSet = new DataSet();

        Node n1 = new Node(new EastNorth(0, 0));
        Node n2 = new Node(new EastNorth(-1, 1));
        Node n3 = new Node(new EastNorth(1, 1));
        dataSet.addPrimitive(n1);
        dataSet.addPrimitive(n2);
        dataSet.addPrimitive(n3);

        Way w = new Way(); // Way is clockwise
        w.setNodes(Arrays.asList(new Node[] {n1, n2, n3}));
        dataSet.addPrimitive(w);

        dataSet.addSelected(w);

        CreateCircleAction.runOn(dataSet);

        // Expected result: Dataset contain one closed way, clockwise
        Collection<Way> resultingWays = dataSet.getWays();
        assertSame(1, resultingWays.size(), String.format("Expect one way after perform action. %d found", resultingWays.size()));
        Way resultingWay = resultingWays.iterator().next();
        assertTrue(resultingWay.isClosed(), "Resulting way is not closed");
        assertTrue(Geometry.isClockwise(resultingWay), "Found anti-clockwise circle while way was clockwise");
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
     * @throws ReflectiveOperationException if an error occurs
     */
    @Test
    void testTicket7421case1() throws ReflectiveOperationException {
        DataSet dataSet = new DataSet();

        Node n1 = new Node(new EastNorth(0, 0));
        Node n2 = new Node(new EastNorth(-1, 1));
        Node n3 = new Node(new EastNorth(1, 1));
        dataSet.addPrimitive(n1);
        dataSet.addPrimitive(n2);
        dataSet.addPrimitive(n3);

        dataSet.addSelected(n1);
        dataSet.addSelected(n2);
        dataSet.addSelected(n3);

        // Mock left/right hand traffic database
        Field rlCache = RightAndLefthandTraffic.class.getDeclaredField("rlCache");
        ReflectionUtils.setObjectsAccessible(rlCache);
        Object origRlCache = rlCache.get(null);
        rlCache.set(null, new GeoPropertyIndex<>(new ConstantTrafficHand(true), 24));

        try {
            CreateCircleAction.runOn(dataSet);

            // Expected result: Dataset contain one closed way, clockwise
            Collection<Way> resultingWays = dataSet.getWays();
            assertSame(1, resultingWays.size(), String.format("Expect one way after perform action. %d found", resultingWays.size()));
            Way resultingWay = resultingWays.iterator().next();
            assertTrue(resultingWay.isClosed(), "Resulting way is not closed");
            assertTrue(Geometry.isClockwise(resultingWay), "Found anti-clockwise way while traffic is left hand.");
        } finally {
            // Restore left/right hand traffic database
            rlCache.set(null, origRlCache);
        }
    }
}
