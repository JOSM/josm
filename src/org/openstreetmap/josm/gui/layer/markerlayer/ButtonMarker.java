// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.layer.markerlayer;

import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.BorderFactory;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.MapView;

/**
 * Marker class with button look-and-feel.
 * 
 * @author Frederik Ramm <frederik@remote.org>
 *
 */
public class ButtonMarker extends Marker {

	private Rectangle buttonRectangle;
	
	public ButtonMarker(LatLon ll, String buttonImage) {
		super(ll, null, buttonImage);
		buttonRectangle = new Rectangle(0, 0, symbol.getIconWidth(), symbol.getIconHeight());
	}
	
	@Override public boolean containsPoint(Point p) {
		Point screen = Main.map.mapView.getPoint(eastNorth);
		buttonRectangle.setLocation(screen.x+4, screen.y+2);
		return buttonRectangle.contains(p);
	}
	
	@Override public void paint(Graphics g, MapView mv, boolean mousePressed, String show) {
		Point screen = mv.getPoint(eastNorth);
		buttonRectangle.setLocation(screen.x+4, screen.y+2);
		symbol.paintIcon(mv, g, screen.x+4, screen.y+2);
		Border b;
		Point mousePosition = mv.getMousePosition();
		if (mousePosition == null)
			return; // mouse outside the whole window
		
		if (mousePressed) {
			b = BorderFactory.createBevelBorder(BevelBorder.LOWERED);
		} else {
			b = BorderFactory.createBevelBorder(BevelBorder.RAISED);
		}
		Insets inset = b.getBorderInsets(mv);
		Rectangle r = new Rectangle(buttonRectangle);
		r.grow((inset.top+inset.bottom)/2, (inset.left+inset.right)/2);
		b.paintBorder(mv, g, r.x, r.y, r.width, r.height);
	}
}
