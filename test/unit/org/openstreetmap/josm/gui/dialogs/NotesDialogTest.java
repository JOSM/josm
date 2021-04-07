// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;

import javax.swing.JLabel;
import javax.swing.JList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.notes.Note;
import org.openstreetmap.josm.data.notes.NoteComment;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.gui.dialogs.NotesDialog.NoteRenderer;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link NotesDialog}
 */
class NotesDialogTest {

    /**
     * Setup tests
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules josmTestRules = new JOSMTestRules().preferences();

    @Test
    void testMultiLineNoteRendering() {
        Note note = new Note(LatLon.ZERO);
        note.setCreatedAt(Instant.now());
        note.addComment(new NoteComment(Instant.now(), User.createLocalUser(null), "foo\nbar\n\nbaz:\nfoo", null, false));
        assertEquals("0: foo; bar; baz: foo",
                ((JLabel) new NoteRenderer().getListCellRendererComponent(new JList<>(), note, 0, false, false)).getText());
    }
}
