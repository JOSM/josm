// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.mapmode;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.NoteData;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.NoteInputDialog;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.dialogs.NotesDialog;
import org.openstreetmap.josm.gui.util.KeyPressReleaseListener;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Map mode to add a new note. Listens for a mouse click and then
 * prompts the user for text and adds a note to the note layer
 */
public class AddNoteAction extends MapMode implements KeyPressReleaseListener {

    private transient NoteData noteData;

    /**
     * Construct a new map mode.
     * @param mapFrame Map frame to pass to the superconstructor
     * @param data Note data container. Must not be null
     */
    public AddNoteAction(MapFrame mapFrame, NoteData data) {
        super(tr("Add a new Note"), "addnote",
            tr("Add note mode"),
            mapFrame, ImageProvider.getCursor("crosshair", "create_note"));
        CheckParameterUtil.ensureParameterNotNull(data, "data");
        noteData = data;
    }

    @Override
    public String getModeHelpText() {
        return tr("Click the location where you wish to create a new note");
    }

    @Override
    public void enterMode() {
        super.enterMode();
        Main.map.mapView.addMouseListener(this);
        Main.map.keyDetector.addKeyListener(this);
    }

    @Override
    public void exitMode() {
        super.exitMode();
        Main.map.mapView.removeMouseListener(this);
        Main.map.keyDetector.removeKeyListener(this);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (!SwingUtilities.isLeftMouseButton(e)) {
            // allow to pan without distraction
            return;
        }
        Main.map.selectMapMode(Main.map.mapModeSelect);
        LatLon latlon = Main.map.mapView.getLatLon(e.getPoint().x, e.getPoint().y);

        NoteInputDialog dialog = new NoteInputDialog(Main.parent, tr("Create new note"), tr("Create note"));
        dialog.showNoteDialog(tr("Enter a detailed comment to create a note"), NotesDialog.ICON_NEW);

        if (dialog.getValue() != 1) {
            Main.debug("User aborted note creation");
            return;
        }
        String input = dialog.getInputText();
        if (input != null && !input.isEmpty()) {
            noteData.createNote(latlon, input);
        } else {
            Notification notification = new Notification(tr("You must enter a comment to create a new note"));
            notification.setIcon(JOptionPane.WARNING_MESSAGE);
            notification.show();
        }
    }

    @Override
    public void doKeyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            Main.map.selectMapMode(Main.map.mapModeSelect);
        }
    }

    @Override
    public void doKeyReleased(KeyEvent e) {
    }
}
