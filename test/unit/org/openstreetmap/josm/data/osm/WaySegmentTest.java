// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of the {@code WaySegment} class.
 */
class WaySegmentTest {

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    @Test
    void testForNodePair() {
        final DataSet ds = new DataSet();
        final Node n1 = new Node(LatLon.ZERO);
        final Node n2 = new Node(new LatLon(1, 0));
        final Node n3 = new Node(new LatLon(2, 0));
        final Node n4 = new Node(new LatLon(3, 0));
        final Way w = new Way();
        for (OsmPrimitive p : Arrays.asList(n1, n2, n3, n4, w)) {
            ds.addPrimitive(p);
        }
        w.addNode(n1);
        w.addNode(n2);
        w.addNode(n1);
        w.addNode(n3);
        w.addNode(n1);
        w.addNode(n4);
        w.addNode(n1);
        assertEquals(WaySegment.forNodePair(w, n1, n2).getLowerIndex(), 0);
        assertEquals(WaySegment.forNodePair(w, n1, n3).getLowerIndex(), 2);
        assertEquals(WaySegment.forNodePair(w, n1, n4).getLowerIndex(), 4);
        assertEquals(WaySegment.forNodePair(w, n4, n1).getLowerIndex(), 5);
        IllegalArgumentException iae = assertThrows(IllegalArgumentException.class, () -> WaySegment.forNodePair(w, n3, n4));
        assertEquals("Node pair is not part of way!", iae.getMessage());
    }
}
