// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openstreetmap.josm.actions.HelpAction.Helpful;
import org.openstreetmap.josm.data.coor.EastNorth;

class MapSlider extends JSlider implements PropertyChangeListener, ChangeListener, Helpful {
	
    private final MapView mv;
	boolean clicked = false;
	
	public MapSlider(MapView mv) {
		super(0, 20);
		setOpaque(false);
		this.mv = mv;
		addMouseListener(new MouseAdapter(){
			@Override public void mousePressed(MouseEvent e) {
				clicked = true;
			}
			@Override public void mouseReleased(MouseEvent e) {
				clicked = false;
			}
		});
		mv.addPropertyChangeListener("scale", this);
		addChangeListener(this);
	}
	
	public void propertyChange(PropertyChangeEvent evt) {
		if (!getModel().getValueIsAdjusting())
			setValue(this.mv.zoom());
	}
	
	public void stateChanged(ChangeEvent e) {
		if (!clicked)
			return;
		EastNorth pos = MapView.world;
		for (int zoom = 0; zoom < getValue(); ++zoom)
			pos = new EastNorth(pos.east()/2, pos.north()/2);
		if (this.mv.getWidth() < this.mv.getHeight())
			this.mv.zoomTo(this.mv.center, pos.east()*2/(this.mv.getWidth()-20));
		else
			this.mv.zoomTo(this.mv.center, pos.north()*2/(this.mv.getHeight()-20));
	}

	public String helpTopic() {
	    return "MapView/Slider";
    }
}