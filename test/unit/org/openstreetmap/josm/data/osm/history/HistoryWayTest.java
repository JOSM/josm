// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.history;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.testutils.JOSMTestRules;

/**
 * Unit tests for class {@link HistoryWay}.
 */
class HistoryWayTest {

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    private static HistoryWay create(Instant d) {
        return new HistoryWay(
                1,    // id
                2,    // version
                true, // visible
                User.createOsmUser(3, "testuser"),
                4,    // changesetId
                d     // timestamp
                );
    }

    /**
     * Unit test for {@link HistoryWay#HistoryWay}.
     */
    @Test
    void testHistoryWay() {
        Instant d = Instant.now();
        HistoryWay way = create(d);

        assertEquals(1, way.getId());
        assertEquals(2, way.getVersion());
        assertTrue(way.isVisible());
        assertEquals("testuser", way.getUser().getName());
        assertEquals(3, way.getUser().getId());
        assertEquals(4, way.getChangesetId());
        assertEquals(d, way.getInstant());

        assertEquals(0, way.getNumNodes());
    }

    /**
     * Unit test for {@link HistoryWay#getType}.
     */
    @Test
    void testGetType() {
        assertEquals(OsmPrimitiveType.WAY, create(Instant.now()).getType());
    }

    @Test
    void testNodeManipulation() {
        HistoryWay way = create(Instant.now());

        way.addNode(1);
        assertEquals(1, way.getNumNodes());
        assertEquals(1, way.getNodeId(0));
        assertThrows(IndexOutOfBoundsException.class, () -> way.getNodeId(1));

        way.addNode(5);
        assertEquals(2, way.getNumNodes());
        assertEquals(5, way.getNodeId(1));
    }

    @Test
    void testIterating() {
        HistoryWay way = create(Instant.now());

        way.addNode(1);
        way.addNode(2);
        List<Long> ids = way.getNodes();

        assertEquals(2, ids.size());
        assertEquals(1, (long) ids.get(0));
        assertEquals(2, (long) ids.get(1));
    }

    /**
     * Unit test for {@link HistoryWay#getDisplayName}.
     */
    @Test
    void testGetDisplayName() {
        HistoryNameFormatter hnf = DefaultNameFormatter.getInstance();
        HistoryWay way0 = create(Instant.now()); // no node
        HistoryWay way1 = create(Instant.now()); // 1 node
        HistoryWay way2 = create(Instant.now()); // 2 nodes

        way1.addNode(1);
        way2.addNode(1);
        way2.addNode(2);

        // CHECKSTYLE.OFF: SingleSpaceSeparator
        assertEquals("1 (0 nodes)", way0.getDisplayName(hnf));
        assertEquals("1 (1 node)",  way1.getDisplayName(hnf));
        assertEquals("1 (2 nodes)", way2.getDisplayName(hnf));

        Map<String, String> map = new HashMap<>();
        map.put("name", "WayName");

        way0.setTags(map);
        way1.setTags(map);
        way2.setTags(map);

        assertEquals("WayName (0 nodes)", way0.getDisplayName(hnf));
        assertEquals("WayName (1 node)",  way1.getDisplayName(hnf));
        assertEquals("WayName (2 nodes)", way2.getDisplayName(hnf));
        // CHECKSTYLE.ON: SingleSpaceSeparator
    }
}
