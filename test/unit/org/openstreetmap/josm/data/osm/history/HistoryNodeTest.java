// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.history;


import java.util.Date;

import org.junit.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;

import static org.junit.Assert.*;

public class HistoryNodeTest {

    @Test
    public void HistoryNode() {
        Date d = new Date();
        HistoryNode node = new HistoryNode(
                1,
                2,
                true,
                "testuser",
                3,
                4,
                d,
                new LatLon(0,0)
        );

        assertEquals(1, node.getId());
        assertEquals(2, node.getVersion());
        assertEquals(true, node.isVisible());
        assertEquals("testuser", node.getUser());
        assertEquals(3, node.getUid());
        assertEquals(4, node.getChangesetId());
        assertEquals(d, node.getTimestamp());
    }

    @Test
    public void getType() {
        Date d = new Date();
        HistoryNode node = new HistoryNode(
                1,
                2,
                true,
                "testuser",
                3,
                4,
                d,
                new LatLon(0,0)
        );

        assertEquals(OsmPrimitiveType.NODE, node.getType());
    }
}
