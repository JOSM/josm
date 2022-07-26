// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.Users;

/**
 * Unit tests for class {@link ChangesetDiscussionComment}.
 */
@BasicPreferences(true)
@Users
class ChangesetDiscussionCommentTest {
    /**
     * Unit test of {@link ChangesetDiscussionComment} constructor.
     */
    @Test
    void testChangesetDiscussionComment() {
        Instant d = Instant.ofEpochMilli(1000);
        User foo = User.createOsmUser(1, "foo");
        ChangesetDiscussionComment cdc = new ChangesetDiscussionComment(d, foo);
        assertEquals(d, cdc.getDate());
        assertEquals(foo, cdc.getUser());
        assertEquals("ChangesetDiscussionComment [date=1970-01-01T00:00:01Z, user=id:1 name:foo, text='null']", cdc.toString());
    }

    /**
     * Unit test of methods {@link ChangesetDiscussionComment#setText} / {@link ChangesetDiscussionComment#getText}.
     */
    @Test
    void testText() {
        ChangesetDiscussionComment cdc = new ChangesetDiscussionComment(Instant.now(), null);
        cdc.setText("foo");
        assertEquals("foo", cdc.getText());
    }
}
