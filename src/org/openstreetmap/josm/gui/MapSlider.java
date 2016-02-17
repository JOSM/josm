// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openstreetmap.josm.gui.help.Helpful;

class MapSlider extends JSlider implements PropertyChangeListener, ChangeListener, Helpful {

    private static final double zoomStep = 1.1;
    private final MapView mv;
    private boolean preventChange;
    private int lastValue;

    MapSlider(MapView mv) {
        super(0, 150);
        setOpaque(false);
        this.mv = mv;
        mv.addPropertyChangeListener("scale", this);
        addChangeListener(this);
        // Call this manually once so it gets setup correctly
        propertyChange(null);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        double maxScale = this.mv.getMaxScale();
        int zoom = (int) Math.round(Math.log(maxScale/mv.getScale())/Math.log(zoomStep));
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
            double scale = maxScale/Math.pow(zoomStep, getValue());
            double snapped = mv.scaleFloor(scale);
            mv.zoomTo(this.mv.getCenter(), snapped);
        }
        propertyChange(null);
    }

    @Override
    public String helpTopic() {
        return ht("/MapView/Slider");
    }
}
