// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.marktr;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;

import javax.swing.JComponent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.help.Helpful;

public class MapScaler extends JComponent implements Helpful {

    private final NavigatableComponent mv;

    private static final int PADDING_RIGHT = 100;

    public MapScaler(NavigatableComponent mv) {
        this.mv = mv;
        setSize(100+PADDING_RIGHT,30);
        setOpaque(false);
    }

    @Override
    public void paint(Graphics g) {
        String text = mv.getDist100PixelText();
        Rectangle2D bound = g.getFontMetrics().getStringBounds(text, g);
        g.setColor(getColor());
        g.drawLine(0, 5, 99, 5);
        g.drawLine(0, 0, 0, 10);
        g.drawLine(99, 0, 99, 10);
        g.drawLine(49, 3, 49, 7);
        g.drawLine(24, 3, 24, 7);
        g.drawLine(74, 3, 74, 7);
        g.drawString(text, (int)(100-bound.getWidth()/2), 23);
        g.drawString("0", 0, 23);
    }

    static public Color getColor() {
        return Main.pref.getColor(marktr("scale"), Color.white);
    }

    @Override
    public String helpTopic() {
        return ht("/MapView/Scaler");
    }
}
