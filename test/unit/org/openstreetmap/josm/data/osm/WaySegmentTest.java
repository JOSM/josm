// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.coor.LatLon;

public class WaySegmentTest {

    @Before
    public void setUp() throws Exception {
        JOSMFixture.createUnitTestFixture().init();
    }

    @Test
    public void testForNodePair() throws Exception {
        final DataSet ds = new DataSet();
        final Node n1 = new Node(new LatLon(0, 0));
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
        Assert.assertEquals(WaySegment.forNodePair(w, n1, n2).lowerIndex, 0);
        Assert.assertEquals(WaySegment.forNodePair(w, n1, n3).lowerIndex, 2);
        Assert.assertEquals(WaySegment.forNodePair(w, n1, n4).lowerIndex, 4);
        Assert.assertEquals(WaySegment.forNodePair(w, n4, n1).lowerIndex, 5);
        try {
            Assert.assertEquals(WaySegment.forNodePair(w, n3, n4).lowerIndex, 5);
            throw new IllegalStateException("Expecting IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }
}
