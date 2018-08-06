// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.notes.Note;

/**
 * Unit tests of the {@code NoteData} class.
 */
public class NoteDataTest {

    /**
     * Unit test for {@link NoteData#NoteData}
     */
    @Test
    public void testNoteData() {
        NoteData empty = new NoteData();
        assertEquals(0, empty.getNotes().size());
        NoteData notEmpty = new NoteData(Arrays.asList(new Note(LatLon.ZERO)));
        assertEquals(1, notEmpty.getNotes().size());
    }
}
