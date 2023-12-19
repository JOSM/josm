// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.upload;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.notes.Note;
import org.openstreetmap.josm.data.notes.NoteComment;
import org.openstreetmap.josm.data.osm.NoteData;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.testutils.FakeOsmApi;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.OsmApi;
import org.openstreetmap.josm.tools.Logging;

import mockit.Mock;
import mockit.MockUp;

/**
 * Test class for {@link UploadNotesTask}
 * @author Taylor Smock
 */
@BasicPreferences
@OsmApi(OsmApi.APIType.FAKE)
class UploadNotesTaskTest {
    static Stream<Arguments> testUpload() {
        final NoteData commonData = new NoteData();
        for (int i = 0; i < 12; i++) {
            for (Note.State state : Note.State.values()) {
                final Note note1 = new Note(LatLon.ZERO);
                note1.setId((state.ordinal() + 1) * (i + 1));
                note1.setCreatedAt(Instant.ofEpochSecond(TimeUnit.DAYS.toSeconds(365) * i));
                note1.setState(state);
                if (i > 2) {
                    note1.addComment(new NoteComment(note1.getCreatedAt().plusSeconds(60),
                            User.getAnonymous(), state.toString() + i, NoteComment.Action.OPENED, false));
                }
                if (i > 4) {
                    note1.addComment(new NoteComment(note1.getCreatedAt().plusSeconds(120),
                            User.getAnonymous(), state.toString() + i, NoteComment.Action.COMMENTED, false));
                }
                if (i > 6) {
                    Instant closedAt = note1.getCreatedAt().plusSeconds(180);
                    note1.addComment(new NoteComment(closedAt,
                            User.getAnonymous(), state.toString() + i, NoteComment.Action.CLOSED, false));
                    note1.setClosedAt(closedAt);
                    note1.setState(Note.State.CLOSED);
                }
                if (i > 8) {
                    note1.addComment(new NoteComment(note1.getCreatedAt().plusSeconds(240),
                            User.getAnonymous(), state.toString() + i, NoteComment.Action.REOPENED, false));
                    note1.setClosedAt(null);
                    note1.setState(Note.State.OPEN);
                }
                if (i > 10) {
                    note1.addComment(new NoteComment(note1.getCreatedAt().plusSeconds(300),
                            User.getAnonymous(), state.toString() + i, NoteComment.Action.HIDDEN, false));
                }
                commonData.addNotes(Collections.singleton(note1));
            }
        }
        return Stream.of(
                Arguments.of(new NoteData(commonData.getNotes()), Collections.singleton(generateNote(1, null, null,
                        new NoteComment.Action[] {NoteComment.Action.OPENED}, new boolean[] {true}))),
                Arguments.of(new NoteData(commonData.getNotes()), Collections.singleton(generateNote(2, Instant.now(), null,
                        new NoteComment.Action[] {NoteComment.Action.OPENED, NoteComment.Action.COMMENTED}, new boolean[] {false, true}))),
                Arguments.of(new NoteData(commonData.getNotes()), Collections.singleton(generateNote(3, Instant.now(),
                        Instant.now().plusSeconds(60), new NoteComment.Action[] {NoteComment.Action.OPENED,
                                NoteComment.Action.COMMENTED, NoteComment.Action.CLOSED}, new boolean[] {false, false, true}))),
                Arguments.of(new NoteData(commonData.getNotes()), Collections.singleton(generateNote(4, Instant.now(),
                        Instant.now().plusSeconds(60), new NoteComment.Action[] {NoteComment.Action.OPENED,
                                NoteComment.Action.COMMENTED, NoteComment.Action.CLOSED, NoteComment.Action.REOPENED},
                        new boolean[] {false, false, false, true})))
        );
    }

    private static Note generateNote(int id, Instant openedAt, Instant closedAt, NoteComment.Action[] actions, boolean[] isNew) {
        final Note newNote = new Note(LatLon.ZERO);
        newNote.setId(id);
        if (openedAt != null) {
            newNote.setState(Note.State.OPEN);
            newNote.setCreatedAt(openedAt);
        } else {
            openedAt = Instant.now();
        }
        if (closedAt != null) {
            newNote.setState(Note.State.CLOSED);
            newNote.setClosedAt(closedAt);
        }

        for (int i = 0; i < actions.length; i++) {
            NoteComment.Action action = actions[i];
            newNote.addComment(new NoteComment(openedAt.plusSeconds(30L * i), User.getAnonymous(),
                    action.toString() + i, action, isNew[i]));
        }

        return newNote;
    }

    @ParameterizedTest
    @MethodSource
    void testUpload(final NoteData noteData, final Collection<Note> shouldBeUploaded)
            throws ExecutionException, InterruptedException {
        TestUtils.assumeWorkingJMockit();
        Logging.clearLastErrorAndWarnings();
        FakeOsmApiMocker fakeOsmApiMocker = new FakeOsmApiMocker();
        noteData.addNotes(shouldBeUploaded);
        new UploadNotesTask().uploadNotes(noteData, NullProgressMonitor.INSTANCE);
        // Sync both threads.
        MainApplication.worker.submit(() -> { /* Sync worker thread */ }).get();
        GuiHelper.runInEDTAndWait(() -> { /* Sync UI thread */ });
        assertTrue(noteData.getNotes().containsAll(shouldBeUploaded));
        for (Note note : noteData.getNotes()) {
            for (NoteComment comment : note.getComments().stream().filter(NoteComment::isNew).collect(Collectors.toList())) {
                assertTrue(shouldBeUploaded.contains(note));
                NoteComment.Action action = comment.getNoteAction();
                if (action == NoteComment.Action.CLOSED) {
                    assertTrue(fakeOsmApiMocker.closed.contains(note));
                } else if (action == NoteComment.Action.COMMENTED) {
                    assertTrue(fakeOsmApiMocker.commented.contains(note));
                } else if (action == NoteComment.Action.REOPENED) {
                    assertTrue(fakeOsmApiMocker.reopened.contains(note));
                } else if (action == NoteComment.Action.OPENED) {
                    assertTrue(fakeOsmApiMocker.created.stream().anyMatch(n -> n.getFirstComment().getText().equals(comment.getText())));
                }
            }
            if (!shouldBeUploaded.contains(note)) {
                assertAll("All comments should not be new", note.getComments().stream().map(comment -> () -> assertFalse(comment.isNew())));
                assertAll("All comments should not be uploaded",
                        () -> assertFalse(fakeOsmApiMocker.closed.contains(note)),
                        () -> assertFalse(fakeOsmApiMocker.commented.contains(note)),
                        () -> assertFalse(fakeOsmApiMocker.created.contains(note)),
                        () -> assertFalse(fakeOsmApiMocker.reopened.contains(note)));
            }
        }
        assertTrue(Logging.getLastErrorAndWarnings().isEmpty());
    }

    private static class FakeOsmApiMocker extends MockUp<FakeOsmApi> {
        Collection<Note> closed = new ArrayList<>();
        Collection<Note> commented = new ArrayList<>();
        Collection<Note> created = new ArrayList<>();
        Collection<Note> reopened = new ArrayList<>();
        @Mock
        public Note createNote(LatLon latlon, String text, ProgressMonitor monitor) throws OsmTransferException {
            final Note newNote = new Note(latlon);
            this.created.add(newNote);
            newNote.setId(Instant.now().toEpochMilli());
            newNote.setClosedAt(Instant.now());
            newNote.addComment(new NoteComment(Instant.now(), User.getAnonymous(), text, NoteComment.Action.OPENED, false));
            return newNote;
        }

        @Mock
        public Note addCommentToNote(Note note, String comment, ProgressMonitor monitor) throws OsmTransferException {
            this.commented.add(note);
            return note;
        }

        @Mock
        public Note closeNote(Note note, String closeMessage, ProgressMonitor monitor) throws OsmTransferException {
            this.closed.add(note);
            return note;
        }

        @Mock
        public Note reopenNote(Note note, String reactivateMessage, ProgressMonitor monitor) throws OsmTransferException {
            this.reopened.add(note);
            return note;
        }
    }
}
