// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui;

import java.awt.Graphics;
import java.awt.geom.Rectangle2D;

import javax.swing.JComponent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.HelpAction.Helpful;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.tools.ColorHelper;

public class MapScaler extends JComponent implements Helpful {

	private final NavigatableComponent mv;
	private final Projection proj;

	public MapScaler(NavigatableComponent mv, Projection proj) {
		this.mv = mv;
		this.proj = proj;
		setSize(100,30);
		setOpaque(false);
    }

	@Override public void paint(Graphics g) {
		double circum = mv.getScale()*100*proj.scaleFactor()*40041455; // circumference of the earth in meter
		String text = circum > 1000 ? (Math.round(circum/100)/10.0)+"km" : Math.round(circum)+"m";
		g.setColor(ColorHelper.html2color(Main.pref.get("color.scale", "#ffffff")));
		g.drawLine(0, 5, 99, 5);
		g.drawLine(0, 0, 0, 10);
		g.drawLine(99, 0, 99, 10);
		Rectangle2D bound = g.getFontMetrics().getStringBounds(text, g);
		g.drawString(text, (int)(50-bound.getWidth()/2), 23);
    }

	public String helpTopic() {
	    return "MapView/Scaler";
    }
}
