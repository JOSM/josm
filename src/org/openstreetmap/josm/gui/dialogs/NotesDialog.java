// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Image;
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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.DownloadNotesInViewAction;
import org.openstreetmap.josm.actions.UploadNotesAction;
import org.openstreetmap.josm.actions.mapmode.AddNoteAction;
import org.openstreetmap.josm.data.notes.Note;
import org.openstreetmap.josm.data.notes.Note.State;
import org.openstreetmap.josm.data.osm.NoteData;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapView.LayerChangeListener;
import org.openstreetmap.josm.gui.NoteInputDialog;
import org.openstreetmap.josm.gui.NoteSortDialog;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.NoteLayer;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.date.DateUtils;

/**
 * Dialog to display and manipulate notes.
 * @since 7852 (renaming)
 * @since 7608 (creation)
 */
public class NotesDialog extends ToggleDialog implements LayerChangeListener {

    /** Small icon size for use in graphics calculations */
    public static final int ICON_SMALL_SIZE = 16;
    /** Large icon size for use in graphics calculations */
    public static final int ICON_LARGE_SIZE = 24;
    /** 24x24 icon for unresolved notes */
    public static final ImageIcon ICON_OPEN = ImageProvider.get("dialogs/notes", "note_open");
    /** 16x16 icon for unresolved notes */
    public static final ImageIcon ICON_OPEN_SMALL =
            new ImageIcon(ICON_OPEN.getImage().getScaledInstance(ICON_SMALL_SIZE, ICON_SMALL_SIZE, Image.SCALE_SMOOTH));
    /** 24x24 icon for resolved notes */
    public static final ImageIcon ICON_CLOSED = ImageProvider.get("dialogs/notes", "note_closed");
    /** 16x16 icon for resolved notes */
    public static final ImageIcon ICON_CLOSED_SMALL =
            new ImageIcon(ICON_CLOSED.getImage().getScaledInstance(ICON_SMALL_SIZE, ICON_SMALL_SIZE, Image.SCALE_SMOOTH));
    /** 24x24 icon for new notes */
    public static final ImageIcon ICON_NEW = ImageProvider.get("dialogs/notes", "note_new");
    /** 16x16 icon for new notes */
    public static final ImageIcon ICON_NEW_SMALL =
            new ImageIcon(ICON_NEW.getImage().getScaledInstance(ICON_SMALL_SIZE, ICON_SMALL_SIZE, Image.SCALE_SMOOTH));
    /** Icon for note comments */
    public static final ImageIcon ICON_COMMENT = ImageProvider.get("dialogs/notes", "note_comment");

    private NoteTableModel model;
    private JList<Note> displayList;
    private final AddCommentAction addCommentAction;
    private final CloseAction closeAction;
    private final DownloadNotesInViewAction downloadNotesInViewAction;
    private final NewAction newAction;
    private final ReopenAction reopenAction;
    private final SortAction sortAction;
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
        uploadAction = new UploadNotesAction();
        buildDialog();
        MapView.addLayerChangeListener(this);
    }

    private void buildDialog() {
        model = new NoteTableModel();
        displayList = new JList<Note>(model);
        displayList.setCellRenderer(new NoteRenderer());
        displayList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        displayList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (noteData != null) { //happens when layer is deleted while note selected
                    noteData.setSelectedNote(displayList.getSelectedValue());
                }
                updateButtonStates();
            }});
        displayList.addMouseListener(new MouseAdapter() {
            //center view on selected note on double click
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    if (noteData != null && noteData.getSelectedNote() != null) {
                        Main.map.mapView.zoomTo(noteData.getSelectedNote().getLatLon());
                    }
                }
            }
        });

        JPanel pane = new JPanel(new BorderLayout());
        pane.add(new JScrollPane(displayList), BorderLayout.CENTER);

        createLayout(pane, false, Arrays.asList(new SideButton[]{
                new SideButton(downloadNotesInViewAction, false),
                new SideButton(newAction, false),
                new SideButton(addCommentAction, false),
                new SideButton(closeAction, false),
                new SideButton(reopenAction, false),
                new SideButton(sortAction, false),
                new SideButton(uploadAction, false)}));
        updateButtonStates();
    }

    private void updateButtonStates() {
        if (noteData == null || noteData.getSelectedNote() == null) {
            closeAction.setEnabled(false);
            addCommentAction.setEnabled(false);
            reopenAction.setEnabled(false);
        } else if (noteData.getSelectedNote().getState() == State.open) {
            closeAction.setEnabled(true);
            addCommentAction.setEnabled(true);
            reopenAction.setEnabled(false);
        } else { //note is closed
            closeAction.setEnabled(false);
            addCommentAction.setEnabled(false);
            reopenAction.setEnabled(true);
        }
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
    public void activeLayerChange(Layer oldLayer, Layer newLayer) {
        // Do nothing
    }

    @Override
    public void layerAdded(Layer newLayer) {
        if (newLayer instanceof NoteLayer) {
            noteData = ((NoteLayer) newLayer).getNoteData();
            model.setData(noteData.getNotes());
            setNotes(noteData.getSortedNotes());
        }
    }

    @Override
    public void layerRemoved(Layer oldLayer) {
        if (oldLayer instanceof NoteLayer) {
            noteData = null;
            model.clearData();
            if (Main.map.mapMode instanceof AddNoteAction) {
                Main.map.selectMapMode(Main.map.mapModeSelect);
            }
        }
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

        private DefaultListCellRenderer defaultListCellRenderer = new DefaultListCellRenderer();
        private final DateFormat dateFormat = DateUtils.getDateTimeFormat(DateFormat.MEDIUM, DateFormat.SHORT);

        @Override
        public Component getListCellRendererComponent(JList<? extends Note> list, Note note, int index,
                boolean isSelected, boolean cellHasFocus) {
            Component comp = defaultListCellRenderer.getListCellRendererComponent(list, note, index, isSelected, cellHasFocus);
            if (note != null && comp instanceof JLabel) {
                String text = note.getFirstComment().getText();
                String userName = note.getFirstComment().getUser().getName();
                if (userName == null || userName.isEmpty()) {
                    userName = "<Anonymous>";
                }
                String toolTipText = userName + " @ " + dateFormat.format(note.getCreatedAt());
                JLabel jlabel = (JLabel) comp;
                jlabel.setText(note.getId() + ": " +text);
                ImageIcon icon;
                if (note.getId() < 0) {
                    icon = ICON_NEW_SMALL;
                } else if (note.getState() == State.closed) {
                    icon = ICON_CLOSED_SMALL;
                } else {
                    icon = ICON_OPEN_SMALL;
                }
                jlabel.setIcon(icon);
                jlabel.setToolTipText(toolTipText);
            }
            return comp;
        }
    }

    class NoteTableModel extends AbstractListModel<Note> {
        private transient List<Note> data;

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
            putValue(SMALL_ICON, ICON_COMMENT);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Note note = displayList.getSelectedValue();
            if (note == null) {
                JOptionPane.showMessageDialog(Main.map,
                        "You must select a note first",
                        "No note selected",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            NoteInputDialog dialog = new NoteInputDialog(Main.parent, tr("Comment on note"), tr("Add comment"));
            dialog.showNoteDialog(tr("Add comment to note:"), NotesDialog.ICON_COMMENT);
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
            putValue(SMALL_ICON, ICON_CLOSED);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            NoteInputDialog dialog = new NoteInputDialog(Main.parent, tr("Close note"), tr("Close note"));
            dialog.showNoteDialog(tr("Close note with message:"), NotesDialog.ICON_CLOSED);
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
            putValue(SMALL_ICON, ICON_NEW);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (noteData == null) { //there is no notes layer. Create one first
                Main.map.mapView.addLayer(new NoteLayer());
            }
            Main.map.selectMapMode(new AddNoteAction(Main.map, noteData));
        }
    }

    class ReopenAction extends AbstractAction {

        /**
         * Constructs a new {@code ReopenAction}.
         */
        ReopenAction() {
            putValue(SHORT_DESCRIPTION, tr("Reopen note"));
            putValue(NAME, tr("Reopen"));
            putValue(SMALL_ICON, ICON_OPEN);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            NoteInputDialog dialog = new NoteInputDialog(Main.parent, tr("Reopen note"), tr("Reopen note"));
            dialog.showNoteDialog(tr("Reopen note with message:"), NotesDialog.ICON_OPEN);
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
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "sort"));
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
}
