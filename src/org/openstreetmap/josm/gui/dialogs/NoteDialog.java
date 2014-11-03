// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.UploadNotesAction;
import org.openstreetmap.josm.actions.mapmode.AddNoteAction;
import org.openstreetmap.josm.data.notes.Note;
import org.openstreetmap.josm.data.notes.Note.State;
import org.openstreetmap.josm.data.osm.NoteData;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapView.LayerChangeListener;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.NoteLayer;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Dialog to display and manipulate notes
 */
public class NoteDialog extends ToggleDialog implements LayerChangeListener {


    /** Small icon size for use in graphics calculations */
    public static final int ICON_SMALL_SIZE = 16;
    /** Large icon size for use in graphics calculations */
    public static final int ICON_LARGE_SIZE = 24;
    /** 24x24 icon for unresolved notes */
    public static final ImageIcon ICON_OPEN = ImageProvider.get("dialogs/notes", "note_open.png");
    /** 16x16 icon for unresolved notes */
    public static final ImageIcon ICON_OPEN_SMALL =
            new ImageIcon(ICON_OPEN.getImage().getScaledInstance(ICON_SMALL_SIZE, ICON_SMALL_SIZE, Image.SCALE_SMOOTH));
    /** 24x24 icon for resolved notes */
    public static final ImageIcon ICON_CLOSED = ImageProvider.get("dialogs/notes", "note_closed.png");
    /** 16x16 icon for resolved notes */
    public static final ImageIcon ICON_CLOSED_SMALL =
            new ImageIcon(ICON_CLOSED.getImage().getScaledInstance(ICON_SMALL_SIZE, ICON_SMALL_SIZE, Image.SCALE_SMOOTH));
    /** 24x24 icon for new notes */
    public static final ImageIcon ICON_NEW = ImageProvider.get("dialogs/notes", "note_new.png");
    /** 16x16 icon for new notes */
    public static final ImageIcon ICON_NEW_SMALL =
            new ImageIcon(ICON_NEW.getImage().getScaledInstance(ICON_SMALL_SIZE, ICON_SMALL_SIZE, Image.SCALE_SMOOTH));
    /** Icon for note comments */
    public static final ImageIcon ICON_COMMENT = ImageProvider.get("dialogs/notes", "note_comment.png");

    private NoteTableModel model;
    private JList<Note> displayList;
    private final AddCommentAction addCommentAction;
    private final CloseAction closeAction;
    private final NewAction newAction;
    private final ReopenAction reopenAction;
    private final UploadNotesAction uploadAction;

    private NoteData noteData;

    /** Creates a new toggle dialog for notes */
    public NoteDialog() {
        super("Notes", "notes/note_open.png", "List of notes", null, 150);
        if (Main.isDebugEnabled()) {
            Main.debug("constructed note dialog");
        }

        addCommentAction = new AddCommentAction();
        closeAction = new CloseAction();
        newAction = new NewAction();
        reopenAction = new ReopenAction();
        uploadAction = new UploadNotesAction();
        buildDialog();
    }

    @Override
    public void showDialog() {
        super.showDialog();
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

        JPanel pane = new JPanel(new BorderLayout());
        pane.add(new JScrollPane(displayList), BorderLayout.CENTER);

        createLayout(pane, false, Arrays.asList(new SideButton[]{
                new SideButton(newAction, false),
                new SideButton(addCommentAction, false),
                new SideButton(closeAction, false),
                new SideButton(reopenAction, false),
                new SideButton(uploadAction, false)}));
        updateButtonStates();
    }

    private void updateButtonStates() {
        if (noteData == null || noteData.getSelectedNote() == null) {
            closeAction.setEnabled(false);
            addCommentAction.setEnabled(false);
            reopenAction.setEnabled(false);
        } else if (noteData.getSelectedNote().getState() == State.open){
            closeAction.setEnabled(true);
            addCommentAction.setEnabled(true);
            reopenAction.setEnabled(false);
        } else { //note is closed
            closeAction.setEnabled(false);
            addCommentAction.setEnabled(false);
            reopenAction.setEnabled(true);
        }
        if(noteData == null || !noteData.isModified()) {
            uploadAction.setEnabled(false);
        } else {
            uploadAction.setEnabled(true);
        }
    }

    @Override
    public void showNotify() {
        MapView.addLayerChangeListener(this);
    }

    @Override
    public void hideNotify() {
        MapView.removeLayerChangeListener(this);
    }

    @Override
    public void activeLayerChange(Layer oldLayer, Layer newLayer) { }

    @Override
    public void layerAdded(Layer newLayer) {
        if (Main.isDebugEnabled()) {
            Main.debug("layer added: " + newLayer);
        }
        if (newLayer instanceof NoteLayer) {
            if (Main.isDebugEnabled()) {
                Main.debug("note layer added");
            }
            noteData = ((NoteLayer)newLayer).getNoteData();
            model.setData(noteData.getNotes());
        }
    }

    @Override
    public void layerRemoved(Layer oldLayer) {
        if (oldLayer instanceof NoteLayer) {
            if (Main.isDebugEnabled()) {
                Main.debug("note layer removed. Clearing everything");
            }
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
    public void setNoteList(List<Note> noteList) {
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
    }

    private class NoteRenderer implements ListCellRenderer<Note> {

        private DefaultListCellRenderer defaultListCellRenderer = new DefaultListCellRenderer();
        private final SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy kk:mm");

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
                String toolTipText = userName + " @ " + sdf.format(note.getCreatedAt());
                JLabel jlabel = (JLabel)comp;
                jlabel.setText(text);
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
        private List<Note> data;

        public NoteTableModel() {
            data = new ArrayList<Note>();
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

        public void setData(List<Note> noteList) {
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

        public AddCommentAction() {
            putValue(SHORT_DESCRIPTION,tr("Add comment"));
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
            Object userInput = JOptionPane.showInputDialog(Main.map,
                    tr("Add comment to note:"),
                    tr("Add comment"),
                    JOptionPane.QUESTION_MESSAGE,
                    ICON_COMMENT,
                    null,null);
            if (userInput == null) { //user pressed cancel
                return;
            }
            noteData.addCommentToNote(note, userInput.toString());
        }
    }

    class CloseAction extends AbstractAction {

        public CloseAction() {
            putValue(SHORT_DESCRIPTION,tr("Close note"));
            putValue(NAME, tr("Close"));
            putValue(SMALL_ICON, ICON_CLOSED);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Object userInput = JOptionPane.showInputDialog(Main.map,
                    tr("Close note with message:"),
                    tr("Close Note"),
                    JOptionPane.QUESTION_MESSAGE,
                    ICON_CLOSED,
                    null,null);
            if (userInput == null) { //user pressed cancel
                return;
            }
            Note note = displayList.getSelectedValue();
            noteData.closeNote(note, userInput.toString());
        }
    }

    class NewAction extends AbstractAction {

        public NewAction() {
            putValue(SHORT_DESCRIPTION,tr("Create a new note"));
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

        public ReopenAction() {
            putValue(SHORT_DESCRIPTION,tr("Reopen note"));
            putValue(NAME, tr("Reopen"));
            putValue(SMALL_ICON, ICON_OPEN);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Object userInput = JOptionPane.showInputDialog(Main.map,
                    tr("Reopen note with message:"),
                    tr("Reopen note"),
                    JOptionPane.QUESTION_MESSAGE,
                    ICON_OPEN,
                    null,null);
            if (userInput == null) { //user pressed cancel
                return;
            }
            Note note = displayList.getSelectedValue();
            noteData.reOpenNote(note, userInput.toString());
        }
    }
}
