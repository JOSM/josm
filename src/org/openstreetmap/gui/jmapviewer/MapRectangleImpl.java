// License: GPL. For details, see LICENSE file.
package org.openstreetmap.gui.jmapviewer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;

import org.openstreetmap.gui.jmapviewer.interfaces.MapRectangle;

/**
 * @author Vincent
 *
 */
public class MapRectangleImpl implements MapRectangle {

    private Coordinate topLeft;
    private Coordinate bottomRight;
    private Color color;
    private Stroke stroke;

    public MapRectangleImpl(Coordinate topLeft, Coordinate bottomRight) {
        this(topLeft, bottomRight, Color.BLUE, new BasicStroke(2));
    }

    public MapRectangleImpl(Coordinate topLeft, Coordinate bottomRight, Color color, Stroke stroke) {
        this.topLeft = topLeft;
        this.bottomRight = bottomRight;
        this.color = color;
        this.stroke = stroke;
    }

    /* (non-Javadoc)
     * @see org.openstreetmap.gui.jmapviewer.interfaces.MapRectangle#getTopLeft()
     */
    @Override
    public Coordinate getTopLeft() {
        return topLeft;
    }

    /* (non-Javadoc)
     * @see org.openstreetmap.gui.jmapviewer.interfaces.MapRectangle#getBottomRight()
     */
    @Override
    public Coordinate getBottomRight() {
        return bottomRight;
    }

    /* (non-Javadoc)
     * @see org.openstreetmap.gui.jmapviewer.interfaces.MapRectangle#paint(java.awt.Graphics, java.awt.Point, java.awt.Point)
     */
    @Override
    public void paint(Graphics g, Point topLeft, Point bottomRight) {
        // Prepare graphics
        Color oldColor = g.getColor();
        g.setColor(color);
        Stroke oldStroke = null;
        if (g instanceof Graphics2D) {
            Graphics2D g2 = (Graphics2D) g;
            oldStroke = g2.getStroke();
            g2.setStroke(stroke);
        }
        // Draw
        g.drawRect(topLeft.x, topLeft.y, bottomRight.x - topLeft.x, bottomRight.y - topLeft.y);
        // Restore graphics
        g.setColor(oldColor);
        if (g instanceof Graphics2D) {
            ((Graphics2D) g).setStroke(oldStroke);
        }
    }

    @Override
    public String toString() {
        return "MapRectangle from " + topLeft + " to " + bottomRight;
    }
}
