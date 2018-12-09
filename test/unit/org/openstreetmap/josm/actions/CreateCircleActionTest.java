// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.GeoProperty;
import org.openstreetmap.josm.tools.GeoPropertyIndex;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.RightAndLefthandTraffic;
import org.openstreetmap.josm.tools.Utils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link CreateCircleAction}.
 */
public final class CreateCircleActionTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().projection();

    /**
     * Test case: When Create Circle action is performed with a single way selected,
     * circle direction must equals way direction.
     * see #7421
     * @throws ReflectiveOperationException if an error occurs
     */
    @Test
    public void testTicket7421case0() throws ReflectiveOperationException {
        DataSet dataSet = new DataSet();

        Node n1 = new Node(new EastNorth(0, 0));
        Node n2 = new Node(new EastNorth(-1, 1));
        Node n3 = new Node(new EastNorth(1, 1));
        dataSet.addPrimitive(n1);
        dataSet.addPrimitive(n2);
        dataSet.addPrimitive(n3);

        Way w = new Way(); // Way is Clockwize
        w.setNodes(Arrays.asList(new Node[] {n1, n2, n3}));
        dataSet.addPrimitive(w);

        dataSet.addSelected(w);

        CreateCircleAction.runOn(dataSet);

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
     * @throws ReflectiveOperationException if an error occurs
     */
    @Test
    public void testTicket7421case1() throws ReflectiveOperationException {
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
        Utils.setObjectsAccessible(rlCache);
        Object origRlCache = rlCache.get(null);
        rlCache.set(null, new GeoPropertyIndex<>(new ConstantTrafficHand(true), 24));

        try {
            CreateCircleAction.runOn(dataSet);

            // Expected result: Dataset contain one closed way, clockwise
            Collection<Way> resultingWays = dataSet.getWays();
            assertSame(String.format("Expect one way after perform action. %d found", resultingWays.size()),
                       resultingWays.size(), 1);
            Way resultingWay = resultingWays.iterator().next();
            assertTrue("Resulting way is not closed",
                       resultingWay.isClosed());
            assertTrue("Found anti-clockwise way while traffic is left hand.",
                       Geometry.isClockwise(resultingWay));
        } finally {
            // Restore left/right hand traffic database
            rlCache.set(null, origRlCache);
        }
    }
}
