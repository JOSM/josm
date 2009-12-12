// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.history.History;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.tools.WindowGeometry;

public class HistoryBrowserDialogManager implements MapView.LayerChangeListener {
    static private HistoryBrowserDialogManager instance;
    static public HistoryBrowserDialogManager getInstance() {
        if (instance == null) {
            instance = new HistoryBrowserDialogManager();
        }
        return instance;
    }

    private Map<Long, HistoryBrowserDialog> dialogs;

    protected HistoryBrowserDialogManager() {
        dialogs = new HashMap<Long, HistoryBrowserDialog>();
        MapView.addLayerChangeListener(this);
    }

    public boolean existsDialog(long id) {
        return dialogs.containsKey(id);
    }

    public void show(long id, HistoryBrowserDialog dialog) {
        if (dialogs.values().contains(dialog)) {
            show(id);
        } else {
            placeOnScreen(dialog);
            dialog.setVisible(true);
            dialogs.put(id, dialog);
        }
    }

    public void show(long id) {
        if (dialogs.keySet().contains(id)) {
            dialogs.get(id).toFront();
        }
    }

    protected boolean hasDialogWithCloseUpperLeftCorner(Point p) {
        for (HistoryBrowserDialog dialog: dialogs.values()) {
            Point corner = dialog.getLocation();
            if (p.x >= corner.x -5 && corner.x + 5 >= p.x
                    && p.y >= corner.y -5 && corner.y + 5 >= p.y)
                return true;
        }
        return false;
    }

    public void placeOnScreen(HistoryBrowserDialog dialog) {
        WindowGeometry geometry = WindowGeometry.centerOnScreen(new Dimension(800,500));
        geometry.apply(dialog);
        Point p = dialog.getLocation();
        while(hasDialogWithCloseUpperLeftCorner(p)) {
            p.x +=20;
            p.y += 20;
        }
        dialog.setLocation(p);
    }

    public void hide(HistoryBrowserDialog dialog) {
        long id = 0;
        for (long i: dialogs.keySet()) {
            if (dialogs.get(i) == dialog) {
                id = i;
                break;
            }
        }
        if (id > 0) {
            dialogs.remove(id);
        }
        dialog.setVisible(false);
        dialog.dispose();
    }

    /**
     * Hides and destroys all currently visible history browser dialogs
     *
     */
    public void hideAll() {
        ArrayList<HistoryBrowserDialog> dialogs = new ArrayList<HistoryBrowserDialog>();
        dialogs.addAll(this.dialogs.values());
        for (HistoryBrowserDialog dialog: dialogs) {
            dialog.unlinkAsListener();
            hide(dialog);
        }
    }

    public void show(History h) {
        if (h == null)
            return;
        if (existsDialog(h.getId())) {
            show(h.getId());
        } else {
            HistoryBrowserDialog dialog = new HistoryBrowserDialog(h);
            show(h.getId(), dialog);
        }
    }

    /* ----------------------------------------------------------------------------- */
    /* LayerChangeListener                                                           */
    /* ----------------------------------------------------------------------------- */
    public void activeLayerChange(Layer oldLayer, Layer newLayer) {}
    public void layerAdded(Layer newLayer) {}

    public void layerRemoved(Layer oldLayer) {
        // remove all history browsers if the number of layers drops to 0
        //
        if (Main.map.mapView.getNumLayers() == 0) {
            hideAll();
        }
    }
}
