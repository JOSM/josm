// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import javax.swing.JLabel;
import javax.swing.JList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.platform.commons.util.ReflectionUtils;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.notes.Note;
import org.openstreetmap.josm.data.notes.NoteComment;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.ChangesetCache;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.NotesDialog.NoteRenderer;
import org.openstreetmap.josm.gui.layer.NoteLayer;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.Projection;
import org.openstreetmap.josm.testutils.mockers.ExtendedDialogMocker;

/**
 * Unit tests of {@link NotesDialog}
 */
@BasicPreferences
/* Only needed for {@link #testTicket21558} */
@Main
@Projection
class NotesDialogTest {
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

    static Stream<Arguments> testCloseActionGetRelatedChangesetUrls() {
        return Stream.of(
                Arguments.of(1, 0, Collections.singletonList("/note/123")),
                Arguments.of(1, 0, Collections.singletonList("/note/231")),
                Arguments.of(1, 1, Collections.singletonList("/note/1")),
                Arguments.of(1, 2, Arrays.asList("/note/1", "/note/1 again")),
                Arguments.of(1, 2, Arrays.asList("/note/1", "/note/1 again", "/note/12 here")),
                Arguments.of(1, 2, Arrays.asList("/note/1", "/note/12 again", "/note/1 here")),
                Arguments.of(1, 2, Arrays.asList("/note/12", "/note/1 again", "/note/1 here")),
                Arguments.of(1, 3, Arrays.asList("/note/1", "/note/1 again", "/note/1 here")),
                Arguments.of(1, 3, Arrays.asList("note 1", "note 1 again", "note 1 here"))
        );
    }

    @ParameterizedTest
    @MethodSource
    void testCloseActionGetRelatedChangesetUrls(long noteId, int expectedChangesets, List<String> changesetComments) {
        try {
            Config.getPref().put("osm-server.url", null);
            final String[] apiList = {"osm.org", "openstreetmap.org", Config.getUrls().getBaseBrowseUrl()};
            for (int i = 0; i < changesetComments.size(); i++) {
                final String comment = changesetComments.get(i);
                final Changeset cs = new Changeset(i + 1);
                cs.put("comment", apiList[i % 3] + comment);
                ChangesetCache.getInstance().update(cs);
            }
            final List<String> changesetUrls = NotesDialog.getRelatedChangesetUrls(noteId);
            assertEquals(expectedChangesets, changesetUrls.size());
        } finally {
            ChangesetCache.getInstance().clear();
        }
    }
}
