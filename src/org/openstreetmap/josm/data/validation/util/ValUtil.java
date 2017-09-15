// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.util;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.OsmValidator;
import org.openstreetmap.josm.tools.CheckParameterUtil;

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
    public static List<List<Way>> getWaysInCell(Way w, Map<Point2D, List<Way>> cellWays) {
        if (w.getNodesCount() == 0)
            return Collections.emptyList();

        Node n1 = w.getNode(0);
        Node n2 = w.getNode(w.getNodesCount() - 1);

        List<List<Way>> cells = new ArrayList<>(2);
        Set<Point2D> cellNodes = new HashSet<>();
        Point2D cell;
        double griddetail = OsmValidator.getGridDetail();

        // First, round coordinates
        // CHECKSTYLE.OFF: SingleSpaceSeparator
        long x0 = Math.round(n1.getEastNorth().east()  * griddetail);
        long y0 = Math.round(n1.getEastNorth().north() * griddetail);
        long x1 = Math.round(n2.getEastNorth().east()  * griddetail);
        long y1 = Math.round(n2.getEastNorth().north() * griddetail);
        // CHECKSTYLE.ON: SingleSpaceSeparator

        // Start of the way
        cell = new Point2D.Double(x0, y0);
        cellNodes.add(cell);
        cells.add(cellWays.computeIfAbsent(cell, k -> new ArrayList<>()));

        // End of the way
        cell = new Point2D.Double(x1, y1);
        if (!cellNodes.contains(cell)) {
            cellNodes.add(cell);
            cells.add(cellWays.computeIfAbsent(cell, k -> new ArrayList<>()));
        }

        // Then floor coordinates, in case the way is in the border of the cell.
        // CHECKSTYLE.OFF: SingleSpaceSeparator
        x0 = (long) Math.floor(n1.getEastNorth().east()  * griddetail);
        y0 = (long) Math.floor(n1.getEastNorth().north() * griddetail);
        x1 = (long) Math.floor(n2.getEastNorth().east()  * griddetail);
        y1 = (long) Math.floor(n2.getEastNorth().north() * griddetail);
        // CHECKSTYLE.ON: SingleSpaceSeparator

        // Start of the way
        cell = new Point2D.Double(x0, y0);
        if (!cellNodes.contains(cell)) {
            cellNodes.add(cell);
            cells.add(cellWays.computeIfAbsent(cell, k -> new ArrayList<>()));
        }

        // End of the way
        cell = new Point2D.Double(x1, y1);
        if (!cellNodes.contains(cell)) {
            cellNodes.add(cell);
            cells.add(cellWays.computeIfAbsent(cell, k -> new ArrayList<>()));
        }
        return cells;
    }

    /**
     * Returns the coordinates of all cells in a grid that a line between 2 nodes intersects with.
     *
     * @param n1 The first node.
     * @param n2 The second node.
     * @param gridDetail The detail of the grid. Bigger values give smaller
     * cells, but a bigger number of them.
     * @return A list with the coordinates of all cells
     * @throws IllegalArgumentException if n1 or n2 is {@code null} or without coordinates
     */
    public static List<Point2D> getSegmentCells(Node n1, Node n2, double gridDetail) {
        CheckParameterUtil.ensureParameterNotNull(n1, "n1");
        CheckParameterUtil.ensureParameterNotNull(n1, "n2");
        return getSegmentCells(n1.getEastNorth(), n2.getEastNorth(), gridDetail);
    }

    /**
     * Returns the coordinates of all cells in a grid that a line between 2 nodes intersects with.
     *
     * @param en1 The first EastNorth.
     * @param en2 The second EastNorth.
     * @param gridDetail The detail of the grid. Bigger values give smaller
     * cells, but a bigger number of them.
     * @return A list with the coordinates of all cells
     * @throws IllegalArgumentException if en1 or en2 is {@code null}
     * @since 6869
     */
    public static List<Point2D> getSegmentCells(EastNorth en1, EastNorth en2, double gridDetail) {
        CheckParameterUtil.ensureParameterNotNull(en1, "en1");
        CheckParameterUtil.ensureParameterNotNull(en2, "en2");
        List<Point2D> cells = new ArrayList<>();
        double x0 = en1.east() * gridDetail;
        double x1 = en2.east() * gridDetail;
        double y0 = en1.north() * gridDetail + 1;
        double y1 = en2.north() * gridDetail + 1;

        if (x0 > x1) {
            // Move to 1st-4th cuadrants
            double aux;
            aux = x0; x0 = x1; x1 = aux;
            aux = y0; y0 = y1; y1 = aux;
        }

        double dx = x1 - x0;
        double dy = y1 - y0;
        long stepY = y0 <= y1 ? 1 : -1;
        long gridX0 = (long) Math.floor(x0);
        long gridX1 = (long) Math.floor(x1);
        long gridY0 = (long) Math.floor(y0);
        long gridY1 = (long) Math.floor(y1);

        long maxSteps = (gridX1 - gridX0) + Math.abs(gridY1 - gridY0) + 1;
        while ((gridX0 <= gridX1 && (gridY0 - gridY1)*stepY <= 0) && maxSteps-- > 0) {
            cells.add(new Point2D.Double(gridX0, gridY0));

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
