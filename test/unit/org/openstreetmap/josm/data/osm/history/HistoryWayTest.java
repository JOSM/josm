// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.history;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.Logging;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link HistoryWay}.
 */
public class HistoryWayTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    private static HistoryWay create(Date d) {
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
    public void testHistoryWay() {
        Date d = new Date();
        HistoryWay way = create(d);

        assertEquals(1, way.getId());
        assertEquals(2, way.getVersion());
        assertTrue(way.isVisible());
        assertEquals("testuser", way.getUser().getName());
        assertEquals(3, way.getUser().getId());
        assertEquals(4, way.getChangesetId());
        assertEquals(d, way.getTimestamp());

        assertEquals(0, way.getNumNodes());
    }

    /**
     * Unit test for {@link HistoryWay#getType}.
     */
    @Test
    public void testGetType() {
        assertEquals(OsmPrimitiveType.WAY, create(new Date()).getType());
    }

    @Test
    public void testNodeManipulation() {
        HistoryWay way = create(new Date());

        way.addNode(1);
        assertEquals(1, way.getNumNodes());
        assertEquals(1, way.getNodeId(0));
        try {
            way.getNodeId(1);
            fail("expected expection of type " + IndexOutOfBoundsException.class.toString());
        } catch (IndexOutOfBoundsException e) {
            // OK
            Logging.trace(e);
        }

        way.addNode(5);
        assertEquals(2, way.getNumNodes());
        assertEquals(5, way.getNodeId(1));
    }

    @Test
    public void testIterating() {
        HistoryWay way = create(new Date());

        way.addNode(1);
        way.addNode(2);
        ArrayList<Long> ids = new ArrayList<>();
        for (long id : way.getNodes()) {
            ids.add(id);
        }

        assertEquals(2, ids.size());
        assertEquals(1, (long) ids.get(0));
        assertEquals(2, (long) ids.get(1));
    }

    /**
     * Unit test for {@link HistoryWay#getDisplayName}.
     */
    @Test
    public void testGetDisplayName() {
        HistoryNameFormatter hnf = DefaultNameFormatter.getInstance();
        HistoryWay way0 = create(new Date()); // no node
        HistoryWay way1 = create(new Date()); // 1 node
        HistoryWay way2 = create(new Date()); // 2 nodes

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
