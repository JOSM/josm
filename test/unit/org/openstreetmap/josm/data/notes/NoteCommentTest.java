// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.notes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;

/**
 * Unit tests for class {@link NoteComment}.
 */
public class NoteCommentTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void init() {
        JOSMFixture.createUnitTestFixture().init();
    }

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
