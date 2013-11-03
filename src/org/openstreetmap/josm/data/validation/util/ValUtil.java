// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.data.validation.util;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.OsmValidator;

/**
 * Utility class
 *
 * @author frsantos
 */
public final class ValUtil {
    
    private ValUtil() {
        // Hide default constructor for utils classes
    }
    
    /**
     * Returns the start and end cells of a way.
     * @param w The way
     * @param cellWays The map with all cells
     * @return A list with all the cells the way starts or ends
     */
    public static List<List<Way>> getWaysInCell(Way w, Map<Point2D,List<Way>> cellWays) {
        if (w.getNodesCount() == 0)
            return Collections.emptyList();

        Node n1 = w.getNode(0);
        Node n2 = w.getNode(w.getNodesCount() - 1);

        List<List<Way>> cells = new ArrayList<List<Way>>(2);
        Set<Point2D> cellNodes = new HashSet<Point2D>();
        Point2D cell;

        // First, round coordinates
        long x0 = Math.round(n1.getEastNorth().east()  * OsmValidator.griddetail);
        long y0 = Math.round(n1.getEastNorth().north() * OsmValidator.griddetail);
        long x1 = Math.round(n2.getEastNorth().east()  * OsmValidator.griddetail);
        long y1 = Math.round(n2.getEastNorth().north() * OsmValidator.griddetail);

        // Start of the way
        cell = new Point2D.Double(x0, y0);
        cellNodes.add(cell);
        List<Way> ways = cellWays.get(cell);
        if (ways == null) {
            ways = new ArrayList<Way>();
            cellWays.put(cell, ways);
        }
        cells.add(ways);

        // End of the way
        cell = new Point2D.Double(x1, y1);
        if (!cellNodes.contains(cell)) {
            cellNodes.add(cell);
            ways = cellWays.get( cell );
            if (ways == null) {
                ways = new ArrayList<Way>();
                cellWays.put(cell, ways);
            }
            cells.add(ways);
        }

        // Then floor coordinates, in case the way is in the border of the cell.
        x0 = (long) Math.floor(n1.getEastNorth().east()  * OsmValidator.griddetail);
        y0 = (long) Math.floor(n1.getEastNorth().north() * OsmValidator.griddetail);
        x1 = (long) Math.floor(n2.getEastNorth().east()  * OsmValidator.griddetail);
        y1 = (long) Math.floor(n2.getEastNorth().north() * OsmValidator.griddetail);

        // Start of the way
        cell = new Point2D.Double(x0, y0);
        if (!cellNodes.contains(cell)) {
            cellNodes.add(cell);
            ways = cellWays.get(cell);
            if (ways == null) {
                ways = new ArrayList<Way>();
                cellWays.put(cell, ways);
            }
            cells.add(ways);
        }

        // End of the way
        cell = new Point2D.Double(x1, y1);
        if (!cellNodes.contains(cell)) {
            cellNodes.add(cell);
            ways = cellWays.get(cell);
            if (ways == null) {
                ways = new ArrayList<Way>();
                cellWays.put(cell, ways);
            }
            cells.add(ways);
        }
        return cells;
    }

    /**
     * Returns the coordinates of all cells in a grid that a line between 2
     * nodes intersects with.
     *
     * @param n1 The first node.
     * @param n2 The second node.
     * @param gridDetail The detail of the grid. Bigger values give smaller
     * cells, but a bigger number of them.
     * @return A list with the coordinates of all cells
     */
    public static List<Point2D> getSegmentCells(Node n1, Node n2, double gridDetail) {
        List<Point2D> cells = new ArrayList<Point2D>();
        double x0 = n1.getEastNorth().east() * gridDetail;
        double x1 = n2.getEastNorth().east() * gridDetail;
        double y0 = n1.getEastNorth().north() * gridDetail + 1;
        double y1 = n2.getEastNorth().north() * gridDetail + 1;

        if (x0 > x1) {
            // Move to 1st-4th cuadrants
            double aux;
            aux = x0; x0 = x1; x1 = aux;
            aux = y0; y0 = y1; y1 = aux;
        }

        double dx  = x1 - x0;
        double dy  = y1 - y0;
        long stepY = y0 <= y1 ? 1 : -1;
        long gridX0 = (long) Math.floor(x0);
        long gridX1 = (long) Math.floor(x1);
        long gridY0 = (long) Math.floor(y0);
        long gridY1 = (long) Math.floor(y1);

        long maxSteps = (gridX1 - gridX0) + Math.abs(gridY1 - gridY0) + 1;
        while ((gridX0 <= gridX1 && (gridY0 - gridY1)*stepY <= 0) && maxSteps-- > 0) {
            cells.add( new Point2D.Double(gridX0, gridY0));

            // Is the cross between the segment and next vertical line nearer than the cross with next horizontal line?
            // Note: segment line formula: y=dy/dx(x-x1)+y1
            // Note: if dy < 0, must use *bottom* line. If dy > 0, must use upper line
            double scanY = dy/dx * (gridX0 + 1 - x1) + y1 + (dy < 0 ? -1 : 0);
            double scanX = dx/dy * (gridY0 + (dy < 0 ? 0 : 1)*stepY - y1) + x1;

            double distX = Math.pow(gridX0 + 1 - x0, 2) + Math.pow(scanY - y0, 2);
            double distY = Math.pow(scanX - x0, 2) + Math.pow(gridY0 + stepY - y0, 2);

            if (distX < distY) {
                gridX0 += 1;
            } else {
                gridY0 += stepY;
            }
        }
        return cells;
    }
}
