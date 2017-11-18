// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.SaveActionBase;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.notes.Note;
import org.openstreetmap.josm.data.notes.Note.State;
import org.openstreetmap.josm.data.notes.NoteComment;
import org.openstreetmap.josm.data.osm.NoteData;
import org.openstreetmap.josm.data.osm.NoteData.NoteDataUpdateListener;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MainFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.gui.io.AbstractIOTask;
import org.openstreetmap.josm.gui.io.UploadNoteLayerTask;
import org.openstreetmap.josm.gui.io.importexport.NoteExporter;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.io.XmlWriter;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.ColorHelper;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.date.DateUtils;

/**
 * A layer to hold Note objects.
 * @since 7522
 */
public class NoteLayer extends AbstractModifiableLayer implements MouseListener, NoteDataUpdateListener {

    /**
     * Pattern to detect end of sentences followed by another one, or a link, in western script.
     * Group 1 (capturing): period, interrogation mark, exclamation mark
     * Group non capturing: at least one horizontal or vertical whitespace
     * Group 2 (capturing): a letter (any script), or any punctuation
     */
    private static final Pattern SENTENCE_MARKS_WESTERN = Pattern.compile("([\\.\\?\\!])(?:[\\h\\v]+)([\\p{L}\\p{Punct}])");

    /**
     * Pattern to detect end of sentences followed by another one, or a link, in eastern script.
     * Group 1 (capturing): ideographic full stop
     * Group 2 (capturing): a letter (any script), or any punctuation
     */
    private static final Pattern SENTENCE_MARKS_EASTERN = Pattern.compile("(\\u3002)([\\p{L}\\p{Punct}])");

    private final NoteData noteData;

    private Note displayedNote;
    private HtmlPanel displayedPanel;
    private JWindow displayedWindow;

    /**
     * Create a new note layer with a set of notes
     * @param notes A list of notes to show in this layer
     * @param name The name of the layer. Typically "Notes"
     */
    public NoteLayer(Collection<Note> notes, String name) {
        super(name);
        noteData = new NoteData(notes);
        noteData.addNoteDataUpdateListener(this);
    }

    /** Convenience constructor that creates a layer with an empty note list */
    public NoteLayer() {
        this(Collections.<Note>emptySet(), tr("Notes"));
    }

    @Override
    public void hookUpMapView() {
        MainApplication.getMap().mapView.addMouseListener(this);
    }

    @Override
    public synchronized void destroy() {
        MainApplication.getMap().mapView.removeMouseListener(this);
        noteData.removeNoteDataUpdateListener(this);
        hideNoteWindow();
        super.destroy();
    }

    /**
     * Returns the note data store being used by this layer
     * @return noteData containing layer notes
     */
    public NoteData getNoteData() {
        return noteData;
    }

    @Override
    public boolean isModified() {
        return noteData.isModified();
    }

    @Override
    public boolean isUploadable() {
        return true;
    }

    @Override
    public boolean requiresUploadToServer() {
        return isModified();
    }

    @Override
    public boolean isSavable() {
        return true;
    }

    @Override
    public boolean requiresSaveToFile() {
        return getAssociatedFile() != null && isModified();
    }

    @Override
    public void paint(Graphics2D g, MapView mv, Bounds box) {
        final int iconHeight = ImageProvider.ImageSizes.SMALLICON.getAdjustedHeight();
        final int iconWidth = ImageProvider.ImageSizes.SMALLICON.getAdjustedWidth();

        for (Note note : noteData.getNotes()) {
            Point p = mv.getPoint(note.getLatLon());

            ImageIcon icon;
            if (note.getId() < 0) {
                icon = ImageProvider.get("dialogs/notes", "note_new", ImageProvider.ImageSizes.SMALLICON);
            } else if (note.getState() == State.CLOSED) {
                icon = ImageProvider.get("dialogs/notes", "note_closed", ImageProvider.ImageSizes.SMALLICON);
            } else {
                icon = ImageProvider.get("dialogs/notes", "note_open", ImageProvider.ImageSizes.SMALLICON);
            }
            int width = icon.getIconWidth();
            int height = icon.getIconHeight();
            g.drawImage(icon.getImage(), p.x - (width / 2), p.y - height, MainApplication.getMap().mapView);
        }
        Note selectedNote = noteData.getSelectedNote();
        if (selectedNote != null) {
            paintSelectedNote(g, mv, iconHeight, iconWidth, selectedNote);
        } else {
            hideNoteWindow();
        }
    }

    private void hideNoteWindow() {
        if (displayedWindow != null) {
            displayedWindow.setVisible(false);
            for (MouseWheelListener listener : displayedWindow.getMouseWheelListeners()) {
                displayedWindow.removeMouseWheelListener(listener);
            }
            displayedWindow.dispose();
            displayedWindow = null;
            displayedPanel = null;
            displayedNote = null;
        }
    }

    private void paintSelectedNote(Graphics2D g, MapView mv, final int iconHeight, final int iconWidth, Note selectedNote) {
        Point p = mv.getPoint(selectedNote.getLatLon());

        g.setColor(ColorHelper.html2color(Config.getPref().get("color.selected")));
        g.drawRect(p.x - (iconWidth / 2), p.y - iconHeight, iconWidth - 1, iconHeight - 1);

        if (displayedNote != null && !displayedNote.equals(selectedNote)) {
            hideNoteWindow();
        }

        Point screenloc = mv.getLocationOnScreen();
        int tx = screenloc.x + p.x + (iconWidth / 2) + 5;
        int ty = screenloc.y + p.y - iconHeight - 1;

        String text = getNoteToolTip(selectedNote);

        if (displayedWindow == null) {
            displayedPanel = new HtmlPanel(text);
            displayedPanel.setBackground(UIManager.getColor("ToolTip.background"));
            displayedPanel.setForeground(UIManager.getColor("ToolTip.foreground"));
            displayedPanel.setFont(UIManager.getFont("ToolTip.font"));
            displayedPanel.setBorder(BorderFactory.createLineBorder(Color.black));
            displayedPanel.enableClickableHyperlinks();
            fixPanelSize(mv, text);
            displayedWindow = new JWindow((MainFrame) Main.parent);
            displayedWindow.setAutoRequestFocus(false);
            displayedWindow.add(displayedPanel);
            // Forward mouse wheel scroll event to MapMover
            displayedWindow.addMouseWheelListener(e -> mv.getMapMover().mouseWheelMoved(
                    (MouseWheelEvent) SwingUtilities.convertMouseEvent(displayedWindow, e, mv)));
        } else {
            displayedPanel.setText(text);
            fixPanelSize(mv, text);
        }

        displayedWindow.pack();
        displayedWindow.setLocation(tx, ty);
        displayedWindow.setVisible(mv.contains(p));
        displayedNote = selectedNote;
    }

    private void fixPanelSize(MapView mv, String text) {
        Dimension d = displayedPanel.getPreferredSize();
        if (d.width > mv.getWidth() / 2) {
            // To make sure long notes are displayed correctly
            displayedPanel.setText(insertLineBreaks(text));
        }
    }

    /**
     * Inserts HTML line breaks ({@code <br>} at the end of each sentence mark
     * (period, interrogation mark, exclamation mark, ideographic full stop).
     * @param longText a long text that does not fit on a single line without exceeding half of the map view
     * @return text with line breaks
     */
    static String insertLineBreaks(String longText) {
        return SENTENCE_MARKS_WESTERN.matcher(SENTENCE_MARKS_EASTERN.matcher(longText).replaceAll("$1<br>$2")).replaceAll("$1<br>$2");
    }

    /**
     * Returns the HTML-formatted tooltip text for the given note.
     * @param note note to display
     * @return the HTML-formatted tooltip text for the given note
     * @since 13111
     */
    public static String getNoteToolTip(Note note) {
        StringBuilder sb = new StringBuilder("<html>");
        sb.append(tr("Note"))
          .append(' ').append(note.getId());
        for (NoteComment comment : note.getComments()) {
            String commentText = comment.getText();
            //closing a note creates an empty comment that we don't want to show
            if (commentText != null && !commentText.trim().isEmpty()) {
                sb.append("<hr/>");
                String userName = XmlWriter.encode(comment.getUser().getName());
                if (userName == null || userName.trim().isEmpty()) {
                    userName = "&lt;Anonymous&gt;";
                }
                sb.append(userName)
                  .append(" on ")
                  .append(DateUtils.getDateFormat(DateFormat.MEDIUM).format(comment.getCommentTimestamp()))
                  .append(":<br>");
                String htmlText = XmlWriter.encode(comment.getText(), true);
                // encode method leaves us with entity instead of \n
                htmlText = htmlText.replace("&#xA;", "<br>");
                // convert URLs to proper HTML links
                htmlText = htmlText.replaceAll("(https?://\\S+)", "<a href=\"$1\">$1</a>");
                sb.append(htmlText);
            }
        }
        sb.append("</html>");
        String result = sb.toString();
        Logging.debug(result);
        return result;
    }

    @Override
    public Icon getIcon() {
        return ImageProvider.get("dialogs/notes", "note_open", ImageProvider.ImageSizes.SMALLICON);
    }

    @Override
    public String getToolTipText() {
        return trn("{0} note", "{0} notes", noteData.getNotes().size(), noteData.getNotes().size());
    }

    @Override
    public void mergeFrom(Layer from) {
        throw new UnsupportedOperationException("Notes layer does not support merging yet");
    }

    @Override
    public boolean isMergable(Layer other) {
        return false;
    }

    @Override
    public void visitBoundingBox(BoundingXYVisitor v) {
        for (Note note : noteData.getNotes()) {
            v.visit(note.getLatLon());
        }
    }

    @Override
    public Object getInfoComponent() {
        StringBuilder sb = new StringBuilder();
        sb.append(tr("Notes layer"))
          .append('\n')
          .append(tr("Total notes:"))
          .append(' ')
          .append(noteData.getNotes().size())
          .append('\n')
          .append(tr("Changes need uploading?"))
          .append(' ')
          .append(isModified());
        return sb.toString();
    }

    @Override
    public Action[] getMenuEntries() {
        List<Action> actions = new ArrayList<>();
        actions.add(LayerListDialog.getInstance().createShowHideLayerAction());
        actions.add(LayerListDialog.getInstance().createDeleteLayerAction());
        actions.add(new LayerListPopup.InfoAction(this));
        actions.add(new LayerSaveAction(this));
        actions.add(new LayerSaveAsAction(this));
        return actions.toArray(new Action[actions.size()]);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (!SwingUtilities.isLeftMouseButton(e)) {
            return;
        }
        Point clickPoint = e.getPoint();
        double snapDistance = 10;
        double minDistance = Double.MAX_VALUE;
        final int iconHeight = ImageProvider.ImageSizes.SMALLICON.getAdjustedHeight();
        Note closestNote = null;
        for (Note note : noteData.getNotes()) {
            Point notePoint = MainApplication.getMap().mapView.getPoint(note.getLatLon());
            //move the note point to the center of the icon where users are most likely to click when selecting
            notePoint.setLocation(notePoint.getX(), notePoint.getY() - iconHeight / 2);
            double dist = clickPoint.distanceSq(notePoint);
            if (minDistance > dist && clickPoint.distance(notePoint) < snapDistance) {
                minDistance = dist;
                closestNote = note;
            }
        }
        noteData.setSelectedNote(closestNote);
    }

    @Override
    public File createAndOpenSaveFileChooser() {
        return SaveActionBase.createAndOpenSaveFileChooser(tr("Save Note file"), NoteExporter.FILE_FILTER);
    }

    @Override
    public AbstractIOTask createUploadTask(ProgressMonitor monitor) {
        return new UploadNoteLayerTask(this, monitor);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        // Do nothing
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // Do nothing
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // Do nothing
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // Do nothing
    }

    @Override
    public void noteDataUpdated(NoteData data) {
        invalidate();
    }

    @Override
    public void selectedNoteChanged(NoteData noteData) {
        invalidate();
    }
}
