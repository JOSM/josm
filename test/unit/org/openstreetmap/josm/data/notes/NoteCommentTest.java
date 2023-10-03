// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.notes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for class {@link NoteComment}.
 */
class NoteCommentTest {
    /**
     * Unit test of {@link NoteComment} class.
     */
    @Test
    void testNoteComment() {
        NoteComment comment = new NoteComment(Instant.now(), null, "foo", null, true);
        assertEquals("foo", comment.toString());
        assertTrue(comment.isNew());
        comment.setNew(false);
        assertFalse(comment.isNew());
    }
}
