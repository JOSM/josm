// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link TagsChangedEvent} class.
 */
class TagsChangedEventTest {
    /**
     * Unit test of {@link TagsChangedEvent#toString}.
     */
    @Test
    void testToString() {
        assertEquals("TAGS_CHANGED", new TagsChangedEvent(null, null, null).toString());
    }
}
