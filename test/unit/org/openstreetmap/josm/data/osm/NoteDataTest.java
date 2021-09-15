// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.notes.Note;
import org.openstreetmap.josm.data.notes.Note.State;
import org.openstreetmap.josm.data.notes.NoteComment;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests of the {@code NoteData} class.
 */
@BasicPreferences
class NoteDataTest {
    /**
     * Unit test for {@link NoteData#NoteData}
     */
    @Test
    void testNoteData() {
        NoteData empty = new NoteData();
        assertEquals(0, empty.getNotes().size());
        NoteData notEmpty = new NoteData(Arrays.asList(new Note(LatLon.ZERO)));
        assertEquals(1, notEmpty.getNotes().size());
    }

    /**
     * Unit test for {@link NoteData#closeNote}
     */
    @Test
    void testCloseNote_nominal() {
        Note note = new Note(LatLon.ZERO);
        note.setState(State.OPEN);
        assertNull(note.getClosedAt());
        assertTrue(note.getComments().isEmpty());

        NoteData data = new NoteData(Arrays.asList(note));
        data.closeNote(note, "foo");

        assertEquals(State.CLOSED, note.getState());
        assertNotNull(note.getClosedAt());
        List<NoteComment> comments = note.getComments();
        assertEquals(1, comments.size());
        NoteComment comment = comments.get(0);
        assertEquals("foo", comment.getText());
        assertEquals(note.getClosedAt(), comment.getCommentTimestamp());
    }

    /**
     * Checks that closeNote does not throw NPE on null arguments
     */
    @Test
    void testCloseNote_nullsafe() {
        assertEquals("Note to close must be in layer",
                assertThrows(IllegalArgumentException.class,
                        () -> new NoteData().closeNote(null, null)).getMessage());
    }
}
