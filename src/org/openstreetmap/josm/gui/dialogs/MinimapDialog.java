// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.bbox.BBoxChooser;
import org.openstreetmap.josm.gui.bbox.SlippyMapBBoxChooser;

/**
 * A small map of the current edit location implemented as {@link ToggleDialog}.
 */
public class MinimapDialog extends ToggleDialog implements NavigatableComponent.ZoomChangeListener, PropertyChangeListener {

    protected final SlippyMapBBoxChooser slippyMap = new SlippyMapBBoxChooser();
    protected boolean skipEvents;

    /**
     * Constructs a new {@code MinimapDialog}.
     */
    public MinimapDialog() {
        super(tr("Mini map"), "minimap", tr("Displays a small map of the current edit location"), null, 150);
        createLayout(slippyMap, false, Collections.<SideButton>emptyList());
        slippyMap.addPropertyChangeListener(BBoxChooser.BBOX_PROP, this);
    }

    @Override
    public void showDialog() {
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
        if (Main.isDisplayingMapView() && !skipEvents) {
            MapView mv = Main.map.mapView;
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
            Main.map.mapView.zoomTo(slippyMap.getBoundingBox());
            skipEvents = false;
        }
    }
}
