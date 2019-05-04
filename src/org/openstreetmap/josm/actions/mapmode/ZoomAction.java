// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.mapmode;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.SelectionManager;
import org.openstreetmap.josm.gui.SelectionManager.SelectionEnded;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Enable the zoom mode within the MapFrame.
 *
 * Holding down the left mouse button select a rectangle with the same aspect
 * ratio than the current map view.
 * Holding down left and right let the user move the former selected rectangle.
 * Releasing the left button zoom to the selection.
 *
 * Rectangle selections with either height or width smaller than 3 pixels
 * are ignored.
 *
 * @author imi
 * @since 1
 */
public class ZoomAction extends MapMode implements SelectionEnded {

    /**
     * Manager that manages the selection rectangle with the aspect ratio of the MapView.
     */
    private final transient SelectionManager selectionManager;

    /**
     * Construct a ZoomAction without a label.
     * @param mapFrame The MapFrame, whose zoom mode should be enabled.
     */
    public ZoomAction(MapFrame mapFrame) {
        super(tr("Zoom mode"), "zoom", tr("Zoom and move map"),
                Shortcut.registerShortcut("mapmode:zoom", tr("Mode: {0}", tr("Zoom mode")), KeyEvent.CHAR_UNDEFINED, Shortcut.NONE),
                ImageProvider.getCursor("normal", "zoom"));
        selectionManager = new SelectionManager(this, true, mapFrame.mapView);
    }

    /**
     * Zoom to the rectangle on the map.
     */
    @Override
    public void selectionEnded(Rectangle r, MouseEvent e) {
        if (r.width >= 3 && r.height >= 3 && MainApplication.isDisplayingMapView()) {
            MapView mv = MainApplication.getMap().mapView;
            mv.zoomToFactor(mv.getEastNorth(r.x+r.width/2, r.y+r.height/2), r.getWidth()/mv.getWidth());
        }
    }

    @Override public void enterMode() {
        super.enterMode();
        selectionManager.register(MainApplication.getMap().mapView, false);
    }

    @Override public void exitMode() {
        super.exitMode();
        selectionManager.unregister(MainApplication.getMap().mapView);
    }

    @Override public String getModeHelpText() {
        return tr("Zoom by dragging or Ctrl+. or Ctrl+,; move with Ctrl+up, left, down, right; move zoom with right button");
    }
}
