// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.mapmode;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Objects;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.NoteData;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.NoteInputDialog;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.util.KeyPressReleaseListener;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;

/**
 * Map mode to add a new note. Listens for a mouse click and then
 * prompts the user for text and adds a note to the note layer
 */
public class AddNoteAction extends MapMode implements KeyPressReleaseListener {

    private final transient NoteData noteData;

    /**
     * Construct a new map mode.
     * @param data Note data container. Must not be null
     * @since 11713
     */
    public AddNoteAction(NoteData data) {
        super(tr("Add a new Note"), "addnote", tr("Add note mode"),
            ImageProvider.getCursor("crosshair", "create_note"));
        noteData = Objects.requireNonNull(data, "data");
    }

    @Override
    public String getModeHelpText() {
        return tr("Click the location where you wish to create a new note");
    }

    @Override
    public void enterMode() {
        super.enterMode();
        MapFrame map = MainApplication.getMap();
        map.mapView.addMouseListener(this);
        map.keyDetector.addKeyListener(this);
    }

    @Override
    public void exitMode() {
        super.exitMode();
        MapFrame map = MainApplication.getMap();
        map.mapView.removeMouseListener(this);
        map.keyDetector.removeKeyListener(this);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (!SwingUtilities.isLeftMouseButton(e)) {
            // allow to pan without distraction
            return;
        }
        MapFrame map = MainApplication.getMap();
        map.selectMapMode(map.mapModeSelect);

        NoteInputDialog dialog = new NoteInputDialog(MainApplication.getMainFrame(), tr("Create a new note"), tr("Create note"));
        dialog.showNoteDialog(tr("Enter a detailed comment to create a note"), ImageProvider.get("dialogs/notes", "note_new"));

        if (dialog.getValue() != 1) {
            Logging.debug("User aborted note creation");
            return;
        }
        String input = dialog.getInputText();
        if (input != null && !input.isEmpty()) {
            LatLon latlon = map.mapView.getLatLon(e.getPoint().x, e.getPoint().y);
            noteData.createNote(latlon, input);
        } else {
            new Notification(tr("You must enter a comment to create a new note")).setIcon(JOptionPane.WARNING_MESSAGE).show();
        }
    }

    @Override
    public void doKeyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            MapFrame map = MainApplication.getMap();
            map.selectMapMode(map.mapModeSelect);
        }
    }

    @Override
    public void doKeyReleased(KeyEvent e) {
        // Do nothing
    }
}
