// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;

import java.awt.Dimension;

import javax.swing.JSlider;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openstreetmap.josm.gui.NavigatableComponent.ZoomChangeListener;
import org.openstreetmap.josm.gui.help.Helpful;
import org.openstreetmap.josm.tools.Destroyable;

/**
 * This is the slider used in the top left corner of the map view. It allows the user to select the scale
 */
class MapSlider extends JSlider implements ZoomChangeListener, ChangeListener, Helpful, Destroyable {

    private static final double ZOOM_STEP = 1.1;
    private final MapView mv;
    private boolean preventChange;
    private int lastValue;

    MapSlider(MapView mv) {
        super(0, 160);
        setOpaque(false);
        this.mv = mv;
        NavigatableComponent.addZoomChangeListener(this);
        addChangeListener(this);
        // Call this manually once so it gets setup correctly
        zoomChanged();
        int w = UIManager.getDefaults().getInt("Slider.thumbWidth") + 150;
        setPreferredSize(new Dimension(w, 27));
    }

    @Override
    public void zoomChanged() {
        double maxScale = this.mv.getMaxScale();
        int zoom = (int) Math.round(Math.log(maxScale/mv.getScale())/Math.log(ZOOM_STEP));
        preventChange = true;
        setValue(zoom);
        lastValue = zoom;
        preventChange = false;
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (preventChange) return;

        if (!getModel().getValueIsAdjusting() && mv.getNativeScaleLayer() != null) {
            if (getValue() < lastValue) {
                mv.zoomOut();
            } else if (getValue() > lastValue) {
                mv.zoomIn();
            }
        } else {
            double maxScale = this.mv.getMaxScale();
            double scale = maxScale/Math.pow(ZOOM_STEP, getValue());
            double snapped = mv.scaleFloor(scale);
            mv.zoomTo(this.mv.getCenter(), snapped);
        }
        zoomChanged();
    }

    @Override
    public String helpTopic() {
        return ht("/MapView/Slider");
    }

    /**
     * Free resources
     */
    public void destroy() {
        NavigatableComponent.removeZoomChangeListener(this);
    }
}
