// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.download;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import java.time.Instant;

/**
 * Unit test of {@link BookmarkList}
 */
class BookmarkListTest {

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

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
