// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.coor.LatLon;

/**
 * Unit tests of the {@code WaySegment} class.
 */
class WaySegmentTest {
    @Test
    void testForNodePair() {
        final DataSet ds = new DataSet();
        final Node n1 = new Node(LatLon.ZERO);
        final Node n2 = new Node(new LatLon(1, 0));
        final Node n3 = new Node(new LatLon(2, 0));
        final Node n4 = new Node(new LatLon(3, 0));
        final Way w1 = new Way();
        final Way w2 = new Way();
        for (OsmPrimitive p : Arrays.asList(n1, n2, n3, n4, w1, w2)) {
            ds.addPrimitive(p);
        }
        w1.addNode(n1);
        w1.addNode(n2);
        w1.addNode(n1);
        w1.addNode(n3);
        w1.addNode(n1);
        w1.addNode(n4);
        w1.addNode(n1);

        w2.addNode(n1);
        w2.addNode(n2);
        w2.addNode(n3);

        assertEquals(0, WaySegment.forNodePair(w1, n1, n2).getLowerIndex());
        assertEquals(2, WaySegment.forNodePair(w1, n1, n3).getLowerIndex());
        assertEquals(4, WaySegment.forNodePair(w1, n1, n4).getLowerIndex());
        assertEquals(5, WaySegment.forNodePair(w1, n4, n1).getLowerIndex());
        // two segments between n3 and n4
        IllegalArgumentException iae = assertThrows(IllegalArgumentException.class, () -> WaySegment.forNodePair(w1, n3, n4));
        assertEquals(IWaySegment.NOT_A_SEGMENT, iae.getMessage());
        // wrong order
        iae = assertThrows(IllegalArgumentException.class, () -> WaySegment.forNodePair(w2, n2, n1));
        assertEquals(IWaySegment.NOT_A_SEGMENT, iae.getMessage());
        // node is not in way
        iae = assertThrows(IllegalArgumentException.class, () -> WaySegment.forNodePair(w2, n1, n4));
        assertEquals(IWaySegment.NOT_A_SEGMENT, iae.getMessage());
    }
}
