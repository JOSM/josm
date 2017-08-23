// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.mapmode;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseEvent;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.layer.markerlayer.PlayHeadMarker;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Singleton marker class to track position of audio.
 *
 * @author david.earl
 *
 */
public class PlayHeadDragMode extends MapMode {

    private boolean dragging;
    private Point mousePos;
    private Point mouseStart;
    private final transient PlayHeadMarker playHeadMarker;

    /**
     * Constructs a new {@code PlayHeadDragMode}.
     * @param m Audio marker
     */
    public PlayHeadDragMode(PlayHeadMarker m) {
        super(tr("Drag play head"), "playheaddrag", tr("Drag play head"), (Shortcut) null,
                Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        playHeadMarker = m;
    }

    @Override public void enterMode() {
        super.enterMode();
        MapFrame map = MainApplication.getMap();
        map.mapView.addMouseListener(this);
        map.mapView.addMouseMotionListener(this);
    }

    @Override public void exitMode() {
        super.exitMode();
        MapFrame map = MainApplication.getMap();
        map.mapView.removeMouseListener(this);
        map.mapView.removeMouseMotionListener(this);
    }

    @Override public void mousePressed(MouseEvent ev) {
        mouseStart = mousePos = ev.getPoint();
    }

    @Override public void mouseDragged(MouseEvent ev) {
        if (mouseStart == null || mousePos == null) return;
        if ((ev.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == 0) return;
        Point p = ev.getPoint();
        if (!dragging) {
            if (p.distance(mouseStart) < 3) return;
            playHeadMarker.startDrag();
            dragging = true;
        }
        if (p.distance(mousePos) == 0) return;
        playHeadMarker.drag(MainApplication.getMap().mapView.getEastNorth(ev.getX(), ev.getY()));
        mousePos = p;
    }

    @Override public void mouseReleased(MouseEvent ev) {
        mouseStart = null;
        if (ev.getButton() != MouseEvent.BUTTON1 || !dragging)
            return;

        requestFocusInMapView();
        updateKeyModifiers(ev);

        EastNorth en = MainApplication.getMap().mapView.getEastNorth(ev.getX(), ev.getY());
        if (!shift) {
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
