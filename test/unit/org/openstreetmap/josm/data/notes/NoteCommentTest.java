// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.notes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link NoteComment}.
 */
public class NoteCommentTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Unit test of {@link NoteComment} class.
     */
    @Test
    public void testNoteComment() {
        NoteComment comment = new NoteComment(new Date(), null, "foo", null, true);
        assertEquals("foo", comment.toString());
        assertTrue(comment.isNew());
        comment.setNew(false);
        assertFalse(comment.isNew());
    }
}
