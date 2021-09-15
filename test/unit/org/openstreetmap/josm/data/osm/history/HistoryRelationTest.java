// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.history;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.RelationMemberData;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests for class {@link HistoryRelation}.
 */
@BasicPreferences
class HistoryRelationTest {
    private static HistoryRelation create(Instant d) {
        return new HistoryRelation(
                1,    // id
                2,    // version
                true, // visible
                User.createOsmUser(3, "testuser"),
                4,    // changesetId
                d     // timestamp
                );
    }

    /**
     * Unit test for {@link HistoryRelation#HistoryRelation}.
     */
    @Test
    void testHistoryRelation() {
        Instant d = Instant.now();
        HistoryRelation rel = create(d);

        assertEquals(1, rel.getId());
        assertEquals(2, rel.getVersion());
        assertTrue(rel.isVisible());
        assertEquals("testuser", rel.getUser().getName());
        assertEquals(3, rel.getUser().getId());
        assertEquals(4, rel.getChangesetId());
        assertEquals(d, rel.getInstant());
    }

    /**
     * Unit test for {@link HistoryRelation#getType}.
     */
    @Test
    void testGetType() {
        assertEquals(OsmPrimitiveType.RELATION, create(Instant.now()).getType());
    }

    /**
     * Unit test for {@link HistoryRelation#getDisplayName}.
     */
    @Test
    void testGetDisplayName() {
        HistoryNameFormatter hnf = DefaultNameFormatter.getInstance();
        HistoryRelation rel0 = create(Instant.now()); // 0 member
        HistoryRelation rel1 = create(Instant.now()); // 1 member
        HistoryRelation rel2 = create(Instant.now()); // 2 members

        rel1.addMember(new RelationMemberData(null, OsmPrimitiveType.NODE, 1));
        rel2.addMember(new RelationMemberData(null, OsmPrimitiveType.NODE, 1));
        rel2.addMember(new RelationMemberData(null, OsmPrimitiveType.NODE, 2));

        // CHECKSTYLE.OFF: SingleSpaceSeparator
        assertEquals("relation (1, 0 members)", rel0.getDisplayName(hnf));
        assertEquals("relation (1, 1 member)",  rel1.getDisplayName(hnf));
        assertEquals("relation (1, 2 members)", rel2.getDisplayName(hnf));

        Map<String, String> map = new HashMap<>();
        map.put("name", "RelName");

        rel0.setTags(map);
        rel1.setTags(map);
        rel2.setTags(map);

        assertEquals("relation (\"RelName\", 0 members)", rel0.getDisplayName(hnf));
        assertEquals("relation (\"RelName\", 1 member)",  rel1.getDisplayName(hnf));
        assertEquals("relation (\"RelName\", 2 members)", rel2.getDisplayName(hnf));
        // CHECKSTYLE.ON: SingleSpaceSeparator
    }
}
