// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.mapmode;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.NoteData;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.dialogs.NoteDialog;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Map mode to add a new note. Listens for a mouse click and then
 * prompts the user for text and adds a note to the note layer
 */
public class AddNoteAction extends MapMode {

    private NoteData noteData;

    /**
     * Construct a new map mode.
     * @param mapFrame Map frame to pass to the superconstructor
     * @param data Note data container. Must not be null
     */
    public AddNoteAction(MapFrame mapFrame, NoteData data) {
        super(tr("Add a new Note"), "addnote.png",
            tr("Add note mode"),
            mapFrame, ImageProvider.getCursor("crosshair", "create_note"));
        if (data == null) {
            throw new IllegalArgumentException("Note data must not be null");
        }
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
    }

    @Override
    public void exitMode() {
        super.exitMode();
        Main.map.mapView.removeMouseListener(this);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        Main.map.selectMapMode(Main.map.mapModeSelect);
        LatLon latlon = Main.map.mapView.getLatLon(e.getPoint().x, e.getPoint().y);
        JLabel label = new JLabel(tr("Enter a comment for a new note"));
        JTextArea textArea = new JTextArea();
        textArea.setRows(6);
        textArea.setColumns(30);
        textArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(textArea);

        Object[] components = new Object[]{label, scrollPane};
        int option = JOptionPane.showConfirmDialog(Main.map,
                components,
                tr("Create new note"),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                NoteDialog.ICON_NEW);
        if (option == JOptionPane.OK_OPTION) {
            String input = textArea.getText();
            if (input != null && !input.isEmpty()) {
                noteData.createNote(latlon, input);
            } else {
                Notification notification = new Notification("You must enter a comment to create a new note");
                notification.setIcon(JOptionPane.WARNING_MESSAGE);
                notification.show();
            }
        }
    }
}
