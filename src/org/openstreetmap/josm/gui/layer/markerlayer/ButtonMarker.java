// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.markerlayer;

import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.BorderFactory;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.template_engine.TemplateEngineDataProvider;

/**
 * Marker class with button look-and-feel.
 *
 * @author Frederik Ramm
 *
 */
public class ButtonMarker extends Marker {

    private final Rectangle buttonRectangle;

    public ButtonMarker(LatLon ll, String buttonImage, MarkerLayer parentLayer, double time, double offset) {
        super(ll, "", buttonImage, parentLayer, time, offset);
        buttonRectangle = new Rectangle(0, 0, symbol.getIconWidth(), symbol.getIconHeight());
    }

    public ButtonMarker(LatLon ll, TemplateEngineDataProvider dataProvider, String buttonImage, MarkerLayer parentLayer, double time,
            double offset) {
        super(ll, dataProvider, buttonImage, parentLayer, time, offset);
        buttonRectangle = new Rectangle(0, 0, symbol.getIconWidth(), symbol.getIconHeight());
    }

    @Override public boolean containsPoint(Point p) {
        Point screen = MainApplication.getMap().mapView.getPoint(this);
        buttonRectangle.setLocation(screen.x+4, screen.y+2);
        return buttonRectangle.contains(p);
    }

    @Override public void paint(Graphics2D g, MapView mv, boolean mousePressed, boolean showTextOrIcon) {
        if (!showTextOrIcon) {
            super.paint(g, mv, mousePressed, showTextOrIcon);
            return;
        }
        Point screen = mv.getPoint(this);
        buttonRectangle.setLocation(screen.x+4, screen.y+2);
        paintIcon(mv, g, screen.x+4, screen.y+2);
        boolean lowered = false;
        if (mousePressed) {
            Point mousePosition = mv.getMousePosition(); // slow and can throw NPE, see JDK-6840067
            // mouse is inside the window
            lowered = mousePosition != null && containsPoint(mousePosition);
        }
        Border b = BorderFactory.createBevelBorder(lowered ? BevelBorder.LOWERED : BevelBorder.RAISED);
        Insets inset = b.getBorderInsets(mv);
        Rectangle r = new Rectangle(buttonRectangle);
        r.grow((inset.top+inset.bottom)/2, (inset.left+inset.right)/2);
        b.paintBorder(mv, g, r.x, r.y, r.width, r.height);

        String labelText = getText();
        if (!labelText.isEmpty() && Config.getPref().getBoolean("marker.buttonlabels", true)) {
            g.drawString(labelText, screen.x+4, screen.y+2);
        }
    }
}
