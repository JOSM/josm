// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.DownloadNotesInViewAction;
import org.openstreetmap.josm.actions.UploadNotesAction;
import org.openstreetmap.josm.actions.mapmode.AddNoteAction;
import org.openstreetmap.josm.data.notes.Note;
import org.openstreetmap.josm.data.notes.Note.State;
import org.openstreetmap.josm.data.notes.NoteComment;
import org.openstreetmap.josm.data.osm.NoteData;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.NoteInputDialog;
import org.openstreetmap.josm.gui.NoteSortDialog;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerAddEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerChangeListener;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerOrderChangeEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerRemoveEvent;
import org.openstreetmap.josm.gui.layer.NoteLayer;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.date.DateUtils;

/**
 * Dialog to display and manipulate notes.
 * @since 7852 (renaming)
 * @since 7608 (creation)
 */
public class NotesDialog extends ToggleDialog implements LayerChangeListener {

    private NoteTableModel model;
    private JList<Note> displayList;
    private final AddCommentAction addCommentAction;
    private final CloseAction closeAction;
    private final DownloadNotesInViewAction downloadNotesInViewAction;
    private final NewAction newAction;
    private final ReopenAction reopenAction;
    private final SortAction sortAction;
    private final OpenInBrowserAction openInBrowserAction;
    private final UploadNotesAction uploadAction;

    private transient NoteData noteData;

    /** Creates a new toggle dialog for notes */
    public NotesDialog() {
        super(tr("Notes"), "notes/note_open", tr("List of notes"), null, 150);
        addCommentAction = new AddCommentAction();
        closeAction = new CloseAction();
        downloadNotesInViewAction = DownloadNotesInViewAction.newActionWithDownloadIcon();
        newAction = new NewAction();
        reopenAction = new ReopenAction();
        sortAction = new SortAction();
        openInBrowserAction = new OpenInBrowserAction();
        uploadAction = new UploadNotesAction();
        buildDialog();
        MainApplication.getLayerManager().addLayerChangeListener(this);
    }

    private void buildDialog() {
        model = new NoteTableModel();
        displayList = new JList<>(model);
        displayList.setCellRenderer(new NoteRenderer());
        displayList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        displayList.addListSelectionListener(e -> {
            if (noteData != null) { //happens when layer is deleted while note selected
                noteData.setSelectedNote(displayList.getSelectedValue());
            }
            updateButtonStates();
        });
        displayList.addMouseListener(new MouseAdapter() {
            //center view on selected note on double click
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2 && noteData != null && noteData.getSelectedNote() != null) {
                    MainApplication.getMap().mapView.zoomTo(noteData.getSelectedNote().getLatLon());
                }
            }
        });

        JPanel pane = new JPanel(new BorderLayout());
        pane.add(new JScrollPane(displayList), BorderLayout.CENTER);

        createLayout(pane, false, Arrays.asList(
                new SideButton(downloadNotesInViewAction, false),
                new SideButton(newAction, false),
                new SideButton(addCommentAction, false),
                new SideButton(closeAction, false),
                new SideButton(reopenAction, false),
                new SideButton(sortAction, false),
                new SideButton(openInBrowserAction, false),
                new SideButton(uploadAction, false)));
        updateButtonStates();
    }

    private void updateButtonStates() {
        if (noteData == null || noteData.getSelectedNote() == null) {
            closeAction.setEnabled(false);
            addCommentAction.setEnabled(false);
            reopenAction.setEnabled(false);
        } else if (noteData.getSelectedNote().getState() == State.OPEN) {
            closeAction.setEnabled(true);
            addCommentAction.setEnabled(true);
            reopenAction.setEnabled(false);
        } else { //note is closed
            closeAction.setEnabled(false);
            addCommentAction.setEnabled(false);
            reopenAction.setEnabled(true);
        }
        openInBrowserAction.setEnabled(noteData != null && noteData.getSelectedNote() != null && noteData.getSelectedNote().getId() > 0);
        if (noteData == null || !noteData.isModified()) {
            uploadAction.setEnabled(false);
        } else {
            uploadAction.setEnabled(true);
        }
        //enable sort button if any notes are loaded
        if (noteData == null || noteData.getNotes().isEmpty()) {
            sortAction.setEnabled(false);
        } else {
            sortAction.setEnabled(true);
        }
    }

    @Override
    public void layerAdded(LayerAddEvent e) {
        if (e.getAddedLayer() instanceof NoteLayer) {
            noteData = ((NoteLayer) e.getAddedLayer()).getNoteData();
            model.setData(noteData.getNotes());
            setNotes(noteData.getSortedNotes());
        }
    }

    @Override
    public void layerRemoving(LayerRemoveEvent e) {
        if (e.getRemovedLayer() instanceof NoteLayer) {
            noteData = null;
            model.clearData();
            MapFrame map = MainApplication.getMap();
            if (map.mapMode instanceof AddNoteAction) {
                map.selectMapMode(map.mapModeSelect);
            }
        }
    }

    @Override
    public void layerOrderChanged(LayerOrderChangeEvent e) {
        // ignored
    }

    /**
     * Sets the list of notes to be displayed in the dialog.
     * The dialog should match the notes displayed in the note layer.
     * @param noteList List of notes to display
     */
    public void setNotes(Collection<Note> noteList) {
        model.setData(noteList);
        updateButtonStates();
        this.repaint();
    }

    /**
     * Notify the dialog that the note selection has changed.
     * Causes it to update or clear its selection in the UI.
     */
    public void selectionChanged() {
        if (noteData == null || noteData.getSelectedNote() == null) {
            displayList.clearSelection();
        } else {
            displayList.setSelectedValue(noteData.getSelectedNote(), true);
        }
        updateButtonStates();
        // TODO make a proper listener mechanism to handle change of note selection
        Main.main.menu.infoweb.noteSelectionChanged();
    }

    /**
     * Returns the currently selected note, if any.
     * @return currently selected note, or null
     * @since 8475
     */
    public Note getSelectedNote() {
        return noteData != null ? noteData.getSelectedNote() : null;
    }

    private static class NoteRenderer implements ListCellRenderer<Note> {

        private final DefaultListCellRenderer defaultListCellRenderer = new DefaultListCellRenderer();
        private final DateFormat dateFormat = DateUtils.getDateTimeFormat(DateFormat.MEDIUM, DateFormat.SHORT);

        @Override
        public Component getListCellRendererComponent(JList<? extends Note> list, Note note, int index,
                boolean isSelected, boolean cellHasFocus) {
            Component comp = defaultListCellRenderer.getListCellRendererComponent(list, note, index, isSelected, cellHasFocus);
            if (note != null && comp instanceof JLabel) {
                NoteComment fstComment = note.getFirstComment();
                JLabel jlabel = (JLabel) comp;
                if (fstComment != null) {
                    String text = note.getFirstComment().getText();
                    String userName = note.getFirstComment().getUser().getName();
                    if (userName == null || userName.isEmpty()) {
                        userName = "<Anonymous>";
                    }
                    String toolTipText = userName + " @ " + dateFormat.format(note.getCreatedAt());
                    jlabel.setToolTipText(toolTipText);
                    jlabel.setText(note.getId() + ": " +text);
                } else {
                    jlabel.setToolTipText(null);
                    jlabel.setText(Long.toString(note.getId()));
                }
                ImageIcon icon;
                if (note.getId() < 0) {
                    icon = ImageProvider.get("dialogs/notes", "note_new", ImageProvider.ImageSizes.SMALLICON);
                } else if (note.getState() == State.CLOSED) {
                    icon = ImageProvider.get("dialogs/notes", "note_closed", ImageProvider.ImageSizes.SMALLICON);
                } else {
                    icon = ImageProvider.get("dialogs/notes", "note_open", ImageProvider.ImageSizes.SMALLICON);
                }
                jlabel.setIcon(icon);
            }
            return comp;
        }
    }

    class NoteTableModel extends AbstractListModel<Note> {
        private final transient List<Note> data;

        /**
         * Constructs a new {@code NoteTableModel}.
         */
        NoteTableModel() {
            data = new ArrayList<>();
        }

        @Override
        public int getSize() {
            if (data == null) {
                return 0;
            }
            return data.size();
        }

        @Override
        public Note getElementAt(int index) {
            return data.get(index);
        }

        public void setData(Collection<Note> noteList) {
            data.clear();
            data.addAll(noteList);
            fireContentsChanged(this, 0, noteList.size());
        }

        public void clearData() {
            displayList.clearSelection();
            data.clear();
            fireIntervalRemoved(this, 0, getSize());
        }
    }

    class AddCommentAction extends AbstractAction {

        /**
         * Constructs a new {@code AddCommentAction}.
         */
        AddCommentAction() {
            putValue(SHORT_DESCRIPTION, tr("Add comment"));
            putValue(NAME, tr("Comment"));
            new ImageProvider("dialogs/notes", "note_comment").getResource().attachImageIcon(this, true);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Note note = displayList.getSelectedValue();
            if (note == null) {
                JOptionPane.showMessageDialog(MainApplication.getMap(),
                        "You must select a note first",
                        "No note selected",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            NoteInputDialog dialog = new NoteInputDialog(Main.parent, tr("Comment on note"), tr("Add comment"));
            dialog.showNoteDialog(tr("Add comment to note:"), ImageProvider.get("dialogs/notes", "note_comment"));
            if (dialog.getValue() != 1) {
                return;
            }
            int selectedIndex = displayList.getSelectedIndex();
            noteData.addCommentToNote(note, dialog.getInputText());
            noteData.setSelectedNote(model.getElementAt(selectedIndex));
        }
    }

    class CloseAction extends AbstractAction {

        /**
         * Constructs a new {@code CloseAction}.
         */
        CloseAction() {
            putValue(SHORT_DESCRIPTION, tr("Close note"));
            putValue(NAME, tr("Close"));
            new ImageProvider("dialogs/notes", "note_closed").getResource().attachImageIcon(this, true);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            NoteInputDialog dialog = new NoteInputDialog(Main.parent, tr("Close note"), tr("Close note"));
            dialog.showNoteDialog(tr("Close note with message:"), ImageProvider.get("dialogs/notes", "note_closed"));
            if (dialog.getValue() != 1) {
                return;
            }
            Note note = displayList.getSelectedValue();
            int selectedIndex = displayList.getSelectedIndex();
            noteData.closeNote(note, dialog.getInputText());
            noteData.setSelectedNote(model.getElementAt(selectedIndex));
        }
    }

    class NewAction extends AbstractAction {

        /**
         * Constructs a new {@code NewAction}.
         */
        NewAction() {
            putValue(SHORT_DESCRIPTION, tr("Create a new note"));
            putValue(NAME, tr("Create"));
            new ImageProvider("dialogs/notes", "note_new").getResource().attachImageIcon(this, true);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (noteData == null) { //there is no notes layer. Create one first
                MainApplication.getLayerManager().addLayer(new NoteLayer());
            }
            MainApplication.getMap().selectMapMode(new AddNoteAction(noteData));
        }
    }

    class ReopenAction extends AbstractAction {

        /**
         * Constructs a new {@code ReopenAction}.
         */
        ReopenAction() {
            putValue(SHORT_DESCRIPTION, tr("Reopen note"));
            putValue(NAME, tr("Reopen"));
            new ImageProvider("dialogs/notes", "note_open").getResource().attachImageIcon(this, true);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            NoteInputDialog dialog = new NoteInputDialog(Main.parent, tr("Reopen note"), tr("Reopen note"));
            dialog.showNoteDialog(tr("Reopen note with message:"), ImageProvider.get("dialogs/notes", "note_open"));
            if (dialog.getValue() != 1) {
                return;
            }

            Note note = displayList.getSelectedValue();
            int selectedIndex = displayList.getSelectedIndex();
            noteData.reOpenNote(note, dialog.getInputText());
            noteData.setSelectedNote(model.getElementAt(selectedIndex));
        }
    }

    class SortAction extends AbstractAction {

        /**
         * Constructs a new {@code SortAction}.
         */
        SortAction() {
            putValue(SHORT_DESCRIPTION, tr("Sort notes"));
            putValue(NAME, tr("Sort"));
            new ImageProvider("dialogs", "sort").getResource().attachImageIcon(this, true);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            NoteSortDialog sortDialog = new NoteSortDialog(Main.parent, tr("Sort notes"), tr("Apply"));
            sortDialog.showSortDialog(noteData.getCurrentSortMethod());
            if (sortDialog.getValue() == 1) {
                noteData.setSortMethod(sortDialog.getSelectedComparator());
            }
        }
    }

    class OpenInBrowserAction extends AbstractAction {
        OpenInBrowserAction() {
            putValue(SHORT_DESCRIPTION, tr("Open the note in an external browser"));
            new ImageProvider("help", "internet").getResource().attachImageIcon(this, true);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final Note note = displayList.getSelectedValue();
            if (note.getId() > 0) {
                final String url = Main.getBaseBrowseUrl() + "/note/" + note.getId();
                OpenBrowser.displayUrl(url);
            }
        }
    }
}
