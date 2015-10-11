// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.history;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.User;

/**
 * Unit tests for class {@link HistoryNode}.
 */
public class HistoryNodeTest {

    @Test
    public void historyNode() {
        Date d = new Date();
        HistoryNode node = new HistoryNode(
                1L,
                2L,
                true,
                User.createOsmUser(3, "testuser"),
                4L,
                d,
                new LatLon(0, 0)
                );

        assertEquals(1, node.getId());
        assertEquals(2, node.getVersion());
        assertTrue(node.isVisible());
        assertEquals("testuser", node.getUser().getName());
        assertEquals(3, node.getUser().getId());
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
                User.createOsmUser(3, "testuser"),
                4,
                d,
                new LatLon(0, 0)
                );

        assertEquals(OsmPrimitiveType.NODE, node.getType());
    }
}
