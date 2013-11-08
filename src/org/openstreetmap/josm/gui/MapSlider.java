// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.gui.help.Helpful;

class MapSlider extends JSlider implements PropertyChangeListener, ChangeListener, Helpful {

    private final MapView mv;
    boolean preventChange = false;

    public MapSlider(MapView mv) {
        super(35, 150);
        setOpaque(false);
        this.mv = mv;
        mv.addPropertyChangeListener("scale", this);
        addChangeListener(this);
        // Call this manually once so it gets setup correctly
        propertyChange(null);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (getModel().getValueIsAdjusting()) return;

        ProjectionBounds world = this.mv.getMaxProjectionBounds();
        ProjectionBounds current = this.mv.getProjectionBounds();

        double cur_e = current.maxEast-current.minEast;
        double cur_n = current.maxNorth-current.minNorth;
        double e = world.maxEast-world.minEast;
        double n = world.maxNorth-world.minNorth;
        int zoom = 0;

        while(zoom <= 150) {
            e /= 1.1;
            n /= 1.1;
            if(e < cur_e && n < cur_n) {
                break;
            }
            ++zoom;
        }
        preventChange=true;
        setValue(zoom);
        preventChange=false;
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (preventChange) return;

        ProjectionBounds world = this.mv.getMaxProjectionBounds();
        double fact = Math.pow(1.1, getValue());
        double es = world.maxEast-world.minEast;
        double n = world.maxNorth-world.minNorth;

        this.mv.zoomTo(new ProjectionBounds(this.mv.getCenter(), es/fact, n/fact));
    }

    @Override
    public String helpTopic() {
        return ht("/MapView/Slider");
    }
}
