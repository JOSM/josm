// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.time.Instant;
import java.util.Collections;

import javax.swing.JLabel;
import javax.swing.JList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.platform.commons.util.ReflectionUtils;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.notes.Note;
import org.openstreetmap.josm.data.notes.NoteComment;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.NotesDialog.NoteRenderer;
import org.openstreetmap.josm.gui.layer.NoteLayer;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.mockers.ExtendedDialogMocker;

import org.openstreetmap.josm.testutils.annotations.Users;

/**
 * Unit tests of {@link NotesDialog}
 */
@BasicPreferences
@Users
class NotesDialogTest {
    /** Only needed for {@link #testTicket21558} */
    @RegisterExtension
    JOSMTestRules rules = new JOSMTestRules().main().projection();
    private Note createMultiLineNote() {
        Note note = new Note(LatLon.ZERO);
        note.setCreatedAt(Instant.now());
        note.addComment(new NoteComment(Instant.now(), User.createLocalUser(null), "foo\nbar\n\nbaz:\nfoo", null, false));
        return note;
    }

    /**
     * Unit test of {@link NoteRenderer}
     */
    @Test
    void testMultiLineNoteRendering() {
        Note note = createMultiLineNote();
        assertEquals("0: foo; bar; baz: foo",
                ((JLabel) new NoteRenderer().getListCellRendererComponent(new JList<>(), note, 0, false, false)).getText());
    }

    /**
     * Unit test of {@link NotesDialog#matchesNote}
     */
    @Test
    void testMatchesNote() {
        Note note = createMultiLineNote();
        assertTrue(NotesDialog.matchesNote(null, note));
        assertTrue(NotesDialog.matchesNote("", note));
        assertTrue(NotesDialog.matchesNote("foo", note));
        assertFalse(NotesDialog.matchesNote("xxx", note));
        assertFalse(NotesDialog.matchesNote("open", note));
        assertFalse(NotesDialog.matchesNote("new", note));
        assertFalse(NotesDialog.matchesNote("reopened", note));
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/21558>#21558</a>
     */
    @Test
    void testTicket21558() throws Exception {
        TestUtils.assumeWorkingJMockit();
        new ExtendedDialogMocker(Collections.singletonMap(tr("Close note"), tr("Close note"))) {
            @Override
            protected String getString(ExtendedDialog instance) {
                return instance.getTitle();
            }
        };
        final NotesDialog notesDialog = new NotesDialog();
        final NotesDialog.CloseAction closeAction = (NotesDialog.CloseAction) ReflectionUtils
                .tryToReadFieldValue(NotesDialog.class, "closeAction", notesDialog).get();
        final JosmTextField filter = (JosmTextField) ReflectionUtils
                .tryToReadFieldValue(NotesDialog.class, "filter", notesDialog).get();
        final NoteLayer noteLayer = new NoteLayer();
        MainApplication.getLayerManager().addLayer(noteLayer);
        final Note note = createMultiLineNote();
        note.setState(Note.State.OPEN);
        noteLayer.getNoteData().addNotes(Collections.singleton(note));
        noteLayer.getNoteData().setSelectedNote(note);
        filter.setText("open");
        assertDoesNotThrow(() -> closeAction.actionPerformed(null));
    }
}
