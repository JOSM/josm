// License: GPL. For details, see LICENSE file.
package org.openstreetmap.gui.jmapviewer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Stroke;
import java.util.List;

import org.openstreetmap.gui.jmapviewer.interfaces.MapPolygon;

/**
 * @author Vincent
 *
 */
public class MapPolygonImpl implements MapPolygon {

    private List<Coordinate> points;
    private Color color;
    private Stroke stroke;

    public MapPolygonImpl(List<Coordinate> points) {
        this(points, Color.BLUE, new BasicStroke(2));
    }

    public MapPolygonImpl(List<Coordinate> points, Color color, Stroke stroke) {
        this.points = points;
        this.color = color;
        this.stroke = stroke;
    }

    /* (non-Javadoc)
     * @see org.openstreetmap.gui.jmapviewer.interfaces.MapPolygon#getPoints()
     */
    @Override
    public List<Coordinate> getPoints() {
        return this.points;
    }

    /* (non-Javadoc)
     * @see org.openstreetmap.gui.jmapviewer.interfaces.MapPolygon#paint(java.awt.Graphics, java.util.List)
     */
    @Override
    public void paint(Graphics g, List<Point> points) {
        Polygon polygon = new Polygon();
        for (Point p : points) {
            polygon.addPoint(p.x, p.y);
        }
        paint(g, polygon);
    }

    /* (non-Javadoc)
     * @see org.openstreetmap.gui.jmapviewer.interfaces.MapPolygon#paint(java.awt.Graphics, java.awt.Polygon)
     */
    @Override
    public void paint(Graphics g, Polygon polygon) {
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
        g.drawPolygon(polygon);
        // Restore graphics
        g.setColor(oldColor);
        if (g instanceof Graphics2D) {
            ((Graphics2D) g).setStroke(oldStroke);
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "MapPolygon [points=" + points + "]";
    }
}
