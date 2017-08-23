// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.bbox.BBoxChooser;
import org.openstreetmap.josm.gui.bbox.SlippyMapBBoxChooser;

/**
 * A small map of the current edit location implemented as {@link ToggleDialog}.
 */
public class MinimapDialog extends ToggleDialog implements NavigatableComponent.ZoomChangeListener, PropertyChangeListener {

    private SlippyMapBBoxChooser slippyMap;
    private boolean skipEvents;

    /**
     * Constructs a new {@code MinimapDialog}.
     */
    public MinimapDialog() {
        super(tr("Mini map"), "minimap", tr("Displays a small map of the current edit location"), null, 150);
    }

    private synchronized void initialize() {
        if (slippyMap != null) {
            return;
        }
        slippyMap = new SlippyMapBBoxChooser();
        createLayout(slippyMap, false, Collections.emptyList());
        slippyMap.setSizeButtonVisible(false);
        slippyMap.addPropertyChangeListener(BBoxChooser.BBOX_PROP, this);
    }

    @Override
    public void showDialog() {
        initialize();
        NavigatableComponent.addZoomChangeListener(this);
        super.showDialog();
    }

    @Override
    public void hideDialog() {
        NavigatableComponent.removeZoomChangeListener(this);
        super.hideDialog();
    }

    @Override
    public void zoomChanged() {
        if (!skipEvents && MainApplication.isDisplayingMapView()) {
            MapView mv = MainApplication.getMap().mapView;
            final Bounds currentBounds = new Bounds(
                    mv.getLatLon(0, mv.getHeight()),
                    mv.getLatLon(mv.getWidth(), 0)
            );
            skipEvents = true;
            slippyMap.setBoundingBox(currentBounds);
            slippyMap.zoomOut(); // to give a better overview
            skipEvents = false;
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (!skipEvents) {
            skipEvents = true;
            MainApplication.getMap().mapView.zoomTo(slippyMap.getBoundingBox());
            skipEvents = false;
        }
    }
}
