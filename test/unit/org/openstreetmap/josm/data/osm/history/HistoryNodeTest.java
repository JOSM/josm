// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.history;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link HistoryNode}.
 */
public class HistoryNodeTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    private static HistoryNode create(Date d) {
        return new HistoryNode(
                1L,   // id
                2L,   // version
                true, // visible
                User.createOsmUser(3, "testuser"),
                4L,   // changesetId
                d,    // timestamp
                LatLon.ZERO
                );
    }

    /**
     * Unit test for {@link HistoryNode#HistoryNode}.
     */
    @Test
    public void testHistoryNode() {
        Date d = new Date();
        HistoryNode node = create(d);

        assertEquals(1, node.getId());
        assertEquals(2, node.getVersion());
        assertTrue(node.isVisible());
        assertEquals("testuser", node.getUser().getName());
        assertEquals(3, node.getUser().getId());
        assertEquals(4, node.getChangesetId());
        assertEquals(d, node.getTimestamp());
    }

    /**
     * Unit test for {@link HistoryNode#getType}.
     */
    @Test
    public void testGetType() {
        assertEquals(OsmPrimitiveType.NODE, create(new Date()).getType());
    }

    /**
     * Unit test for {@link HistoryNode#getCoords}.
     */
    @Test
    public void testGetCoords() {
        Node n = new Node(new LatLon(45, 0));
        n.setOsmId(1, 2);
        n.setUser(User.createOsmUser(3, "testuser"));
        n.setChangesetId(4);
        assertEquals(n.getCoor(), new HistoryNode(n).getCoords());
    }

    /**
     * Unit test for {@link HistoryNode#getDisplayName}.
     */
    @Test
    public void testGetDisplayName() {
        HistoryNode node = create(new Date());
        HistoryNameFormatter hnf = DefaultNameFormatter.getInstance();
        assertEquals("1 (0.0, 0.0)", node.getDisplayName(hnf));
        LatLon ll = node.getCoords();
        node.setCoords(null);
        assertEquals("1", node.getDisplayName(hnf));
        node.setCoords(ll);
        Map<String, String> map = new HashMap<>();
        map.put("name", "NodeName");
        node.setTags(map);
        assertEquals("NodeName (0.0, 0.0)", node.getDisplayName(hnf));
        node.setCoords(null);
        assertEquals("NodeName", node.getDisplayName(hnf));
    }
}
