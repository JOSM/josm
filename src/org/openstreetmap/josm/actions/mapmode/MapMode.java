// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.actions.mapmode;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * A class implementing MapMode is able to be selected as an mode for map editing.
 * As example scrolling the map is a MapMode, connecting Nodes to new Ways
 * is another.
 *
 * MapModes should register/deregister all necessary listeners on the map's view
 * control.
 */
abstract public class MapMode extends JosmAction implements MouseListener, MouseMotionListener {
    private final Cursor cursor;
    private Cursor oldCursor;

    /**
     * Constructor for mapmodes without an menu
     */
    public MapMode(String name, String iconName, String tooltip, Shortcut shortcut, MapFrame mapFrame, Cursor cursor) {
        super(name, "mapmode/"+iconName, tooltip, shortcut, false);
        this.cursor = cursor;
        putValue("active", false);
    }

    /**
     * Constructor for mapmodes with an menu (no shortcut will be registered)
     */
    public MapMode(String name, String iconName, String tooltip, MapFrame mapFrame, Cursor cursor) {
        putValue(NAME, name);
        putValue(SMALL_ICON, ImageProvider.get("mapmode", iconName));
        putValue(SHORT_DESCRIPTION, tooltip);
        this.cursor = cursor;
    }

    public void enterMode() {
        putValue("active", true);
        oldCursor = Main.map.mapView.getCursor();
        Main.map.mapView.setCursor(cursor);
        updateStatusLine();
    }
    public void exitMode() {
        putValue("active", false);
        Main.map.mapView.setCursor(oldCursor);
    }

    protected void updateStatusLine() {
        Main.map.statusLine.setHelpText(getModeHelpText());
        Main.map.statusLine.repaint();
    }

    public String getModeHelpText() {
        return "";
    }
    /**
     * Call selectMapMode(this) on the parent mapFrame.
     */
    public void actionPerformed(ActionEvent e) {
        if (Main.map != null)
            Main.map.selectMapMode(this);
    }

    // By default, all tools will work with all layers. Can be overwritten to require
    // a special type of layer
    public boolean layerIsSupported(Layer l) {
        return true;
    }

    public void mouseReleased(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
    public void mousePressed(MouseEvent e) {}
    public void mouseClicked(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseMoved(MouseEvent e) {}
    public void mouseDragged(MouseEvent e) {}
}
