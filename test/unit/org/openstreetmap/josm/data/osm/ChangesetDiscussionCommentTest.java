// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link ChangesetDiscussionComment}.
 */
class ChangesetDiscussionCommentTest {

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Unit test of {@link ChangesetDiscussionComment} constructor.
     */
    @Test
    void testChangesetDiscussionComment() {
        Date d = new Date(1000);
        User foo = User.createOsmUser(1, "foo");
        ChangesetDiscussionComment cdc = new ChangesetDiscussionComment(d, foo);
        assertEquals(d, cdc.getDate());
        assertEquals(foo, cdc.getUser());
        assertEquals("ChangesetDiscussionComment [date=Thu Jan 01 00:00:01 UTC 1970, user=id:1 name:foo, text='null']", cdc.toString());
    }

    /**
     * Unit test of methods {@link ChangesetDiscussionComment#setText} / {@link ChangesetDiscussionComment#getText}.
     */
    @Test
    void testText() {
        ChangesetDiscussionComment cdc = new ChangesetDiscussionComment(new Date(), null);
        cdc.setText("foo");
        assertEquals("foo", cdc.getText());
    }
}
