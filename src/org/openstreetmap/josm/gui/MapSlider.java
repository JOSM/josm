// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openstreetmap.josm.actions.HelpAction.Helpful;
import org.openstreetmap.josm.data.coor.EastNorth;

class MapSlider extends JSlider implements PropertyChangeListener, ChangeListener, Helpful {

    private final MapView mv;
    boolean preventChange = false;

    public MapSlider(MapView mv) {
        super(35, 150);
        setOpaque(false);
        this.mv = mv;
        mv.addPropertyChangeListener("scale", this);
        addChangeListener(this);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (getModel().getValueIsAdjusting()) return;
        
        double sizex = this.mv.scale * this.mv.getWidth();
        double sizey = this.mv.scale * this.mv.getHeight();
        for (int zoom = 0; zoom <= 150; zoom++, sizex *= 1.1, sizey *= 1.1) {
            if (sizex > MapView.world.east() || sizey > MapView.world.north()) {
                preventChange=true;
                setValue(zoom);
                preventChange=false;
                break;
            }
        }
    }

    public void stateChanged(ChangeEvent e) {
        if (preventChange) return;
        EastNorth pos = MapView.world;
        for (int zoom = 0; zoom < getValue(); zoom++)
            pos = new EastNorth(pos.east()/1.1, pos.north()/1.1);
        if (this.mv.getWidth() < this.mv.getHeight())
            this.mv.zoomTo(this.mv.center, pos.east()/(this.mv.getWidth()-20));
        else
            this.mv.zoomTo(this.mv.center, pos.north()/(this.mv.getHeight()-20));
    }

    public String helpTopic() {
        return "MapView/Slider";
    }
}