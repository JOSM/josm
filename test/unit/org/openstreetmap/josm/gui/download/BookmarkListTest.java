// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.download;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.Changeset;

/**
 * Unit test of {@link BookmarkList}
 */
class BookmarkListTest {
    /**
     * Unit test of {@link BookmarkList.ChangesetBookmark#ChangesetBookmark(Changeset)}
     */
    @Test
    void testChangeset() {
        Changeset cs = new Changeset();
        cs.setCreatedAt(Instant.EPOCH);
        new BookmarkList.ChangesetBookmark(cs);
    }
}
