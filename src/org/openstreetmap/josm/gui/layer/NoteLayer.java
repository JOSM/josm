// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JToolTip;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.SaveActionBase;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.notes.Note;
import org.openstreetmap.josm.data.notes.Note.State;
import org.openstreetmap.josm.data.notes.NoteComment;
import org.openstreetmap.josm.data.osm.NoteData;
import org.openstreetmap.josm.data.osm.NoteData.NoteDataUpdateListener;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.datatransfer.ClipboardUtils;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.gui.io.AbstractIOTask;
import org.openstreetmap.josm.gui.io.UploadNoteLayerTask;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.NoteExporter;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.XmlWriter;
import org.openstreetmap.josm.tools.ColorHelper;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.date.DateUtils;

/**
 * A layer to hold Note objects.
 * @since 7522
 */
public class NoteLayer extends AbstractModifiableLayer implements MouseListener, NoteDataUpdateListener {

    private final NoteData noteData;

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
        Main.map.mapView.addMouseListener(this);
    }

    @Override
    public synchronized void destroy() {
        Main.map.mapView.removeMouseListener(this);
        noteData.removeNoteDataUpdateListener(this);
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
            g.drawImage(icon.getImage(), p.x - (width / 2), p.y - height, Main.map.mapView);
        }
        if (noteData.getSelectedNote() != null) {
            StringBuilder sb = new StringBuilder("<html>");
            sb.append(tr("Note"))
              .append(' ').append(noteData.getSelectedNote().getId());
            for (NoteComment comment : noteData.getSelectedNote().getComments()) {
                String commentText = comment.getText();
                //closing a note creates an empty comment that we don't want to show
                if (commentText != null && !commentText.trim().isEmpty()) {
                    sb.append("<hr/>");
                    String userName = XmlWriter.encode(comment.getUser().getName());
                    if (userName == null || userName.trim().isEmpty()) {
                        userName = "&lt;Anonymous&gt;";
                    }
                    sb.append(userName);
                    sb.append(" on ");
                    sb.append(DateUtils.getDateFormat(DateFormat.MEDIUM).format(comment.getCommentTimestamp()));
                    sb.append(":<br/>");
                    String htmlText = XmlWriter.encode(comment.getText(), true);
                    htmlText = htmlText.replace("&#xA;", "<br/>"); //encode method leaves us with entity instead of \n
                    htmlText = htmlText.replace("/", "/\u200b"); //zero width space to wrap long URLs (see #10864)
                    sb.append(htmlText);
                }
            }
            sb.append("</html>");
            JToolTip toolTip = new JToolTip();
            toolTip.setTipText(sb.toString());
            Point p = mv.getPoint(noteData.getSelectedNote().getLatLon());

            g.setColor(ColorHelper.html2color(Main.pref.get("color.selected")));
            g.drawRect(p.x - (iconWidth / 2), p.y - iconHeight,
                    iconWidth - 1, iconHeight - 1);

            int tx = p.x + (iconWidth / 2) + 5;
            int ty = p.y - iconHeight - 1;
            g.translate(tx, ty);

            //Carried over from the OSB plugin. Not entirely sure why it is needed
            //but without it, the tooltip doesn't get sized correctly
            for (int x = 0; x < 2; x++) {
                Dimension d = toolTip.getUI().getPreferredSize(toolTip);
                d.width = Math.min(d.width, mv.getWidth() / 2);
                if (d.width > 0 && d.height > 0) {
                    toolTip.setSize(d);
                    try {
                        toolTip.paint(g);
                    } catch (IllegalArgumentException e) {
                        // See #11123 - https://bugs.openjdk.java.net/browse/JDK-6719550
                        // Ignore the exception, as Netbeans does: http://hg.netbeans.org/main-silver/rev/c96f4d5fbd20
                        Logging.log(Logging.LEVEL_ERROR, e);
                    }
                }
            }
            g.translate(-tx, -ty);
        }
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
        if (SwingUtilities.isRightMouseButton(e) && noteData.getSelectedNote() != null) {
            final String url = OsmApi.getOsmApi().getBaseUrl() + "notes/" + noteData.getSelectedNote().getId();
            ClipboardUtils.copyString(url);
            return;
        } else if (!SwingUtilities.isLeftMouseButton(e)) {
            return;
        }
        Point clickPoint = e.getPoint();
        double snapDistance = 10;
        double minDistance = Double.MAX_VALUE;
        final int iconHeight = ImageProvider.ImageSizes.SMALLICON.getAdjustedHeight();
        Note closestNote = null;
        for (Note note : noteData.getNotes()) {
            Point notePoint = Main.map.mapView.getPoint(note.getLatLon());
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
        return SaveActionBase.createAndOpenSaveFileChooser(tr("Save GPX file"), NoteExporter.FILE_FILTER);
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
