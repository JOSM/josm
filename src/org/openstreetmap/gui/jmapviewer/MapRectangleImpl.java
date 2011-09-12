// License: GPL. For details, see LICENSE file.
package org.openstreetmap.gui.jmapviewer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;

import org.openstreetmap.gui.jmapviewer.interfaces.MapRectangle;
import org.openstreetmap.josm.data.Bounds;

/**
 * @author Vincent
 *
 */
public class MapRectangleImpl implements MapRectangle {

    private Coordinate topLeft;
    private Coordinate bottomRight;
    Color color;

    public MapRectangleImpl(Bounds bounds) {
        this(bounds, Color.BLUE);
    }

    public MapRectangleImpl(Bounds bounds, Color color) {
        this.topLeft = new Coordinate(bounds.getMax().lat(), bounds.getMin().lon());
        this.bottomRight = new Coordinate(bounds.getMin().lat(), bounds.getMax().lon());
        this.color = color;
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
        g.setColor(color);
        g.drawRect(topLeft.x, topLeft.y, bottomRight.x - topLeft.x, bottomRight.y - topLeft.y);
    }

    @Override
    public String toString() {
        return "MapRectangle from " + topLeft + " to " + bottomRight;
    }
}
