// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.mapmode;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.gui.layer.markerlayer.PlayHeadMarker;

/**
 * Singleton marker class to track position of audio.
 *
 * @author david.earl
 *
 */
public class PlayHeadDragMode extends MapMode {

    private boolean dragging = false;
    private Point mousePos = null;
    private Point mouseStart = null;
    private PlayHeadMarker playHeadMarker = null;

    /**
     * Constructs a new {@code PlayHeadDragMode}.
     * @param m Audio marker
     */
    public PlayHeadDragMode(PlayHeadMarker m) {
        super(tr("Drag play head"), "playheaddrag", tr("Drag play head"), null,
                Main.map, Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        playHeadMarker = m;
    }

    @Override public void enterMode() {
        super.enterMode();
        Main.map.mapView.addMouseListener(this);
        Main.map.mapView.addMouseMotionListener(this);
    }

    @Override public void exitMode() {
        super.exitMode();
        Main.map.mapView.removeMouseListener(this);
        Main.map.mapView.removeMouseMotionListener(this);
    }

    @Override public void mousePressed(MouseEvent ev) {
        mouseStart = mousePos = ev.getPoint();
    }

    @Override public void mouseDragged(MouseEvent ev) {
        if (mouseStart == null || mousePos == null) return;
        if ((ev.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == 0) return;
        Point p = ev.getPoint();
        if (! dragging) {
            if (p.distance(mouseStart) < 3) return;
            playHeadMarker.startDrag();
            dragging = true;
        }
        if (p.distance(mousePos) == 0) return;
        playHeadMarker.drag(Main.map.mapView.getEastNorth(ev.getX(), ev.getY()));
        mousePos = p;
    }

    @Override public void mouseReleased(MouseEvent ev) {
        Point p = ev.getPoint();
        mouseStart = null;
        if (ev.getButton() != MouseEvent.BUTTON1 || p == null || ! dragging)
            return;

        requestFocusInMapView();
        updateKeyModifiers(ev);

        EastNorth en = Main.map.mapView.getEastNorth(ev.getX(), ev.getY());
        if (! shift) {
            playHeadMarker.reposition(en);
        } else {
            playHeadMarker.synchronize(en);
        }
        mousePos = null;
        dragging = false;
    }

    @Override public String getModeHelpText() {
        return tr("Drag play head and release near track to play audio from there; SHIFT+release to synchronize audio at that point.");
    }
}
