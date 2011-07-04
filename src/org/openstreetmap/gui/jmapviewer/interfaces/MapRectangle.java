package org.openstreetmap.gui.jmapviewer.interfaces;

//License: GPL. Copyright 2009 by Stefan Zeller

import java.awt.Graphics;
import java.awt.Point;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.JMapViewer;

/**
 * Interface to be implemented by rectangles that can be displayed on the map.
 *
 * @author Stefan Zeller
 * @see JMapViewer#addMapRectangle(MapRectangle)
 * @see JMapViewer#getMapRectangleList()
 * @date 21.06.2009S
 */
public interface MapRectangle {

    /**
     * @return Latitude/Longitude of top left of rectangle
     */
    public Coordinate getTopLeft();

    /**
     * @return Latitude/Longitude of bottom right of rectangle
     */
    public Coordinate getBottomRight();

    /**
     * Paints the map rectangle on the map. The <code>topLeft</code> and
     * <code>bottomRight</code> are specifying the coordinates within <code>g</code>
     *
     * @param g
     * @param position
     */
    public void paint(Graphics g, Point topLeft, Point bottomRight);
}
