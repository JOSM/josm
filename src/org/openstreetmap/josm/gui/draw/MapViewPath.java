// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.draw;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapViewState;
import org.openstreetmap.josm.gui.MapViewState.MapViewPoint;

/**
 * This is a version of a java Path2D that allows you to add points to it by simply giving their east/north, lat/lon or node coordinates.
 * @author Michael Zangl
 * @since 10875
 */
public class MapViewPath extends MapPath2D {

    private final MapViewState state;

    /**
     * Create a new path
     * @param mv The map view to use for coordinate conversion.
     */
    public MapViewPath(MapView mv) {
        this(mv.getState());
    }

    /**
     * Create a new path
     * @param state The state to use for coordinate conversion.
     */
    public MapViewPath(MapViewState state) {
        this.state = state;
    }

    /**
     * Move the cursor to the given node.
     * @param n The node
     * @return this for easy chaining.
     */
    public MapViewPath moveTo(Node n) {
        moveTo(n.getEastNorth());
        return this;
    }

    /**
     * Move the cursor to the given position.
     * @param eastNorth The position
     * @return this for easy chaining.
     */
    public MapViewPath moveTo(EastNorth eastNorth) {
        moveTo(state.getPointFor(eastNorth));
        return this;
    }

    @Override
    public MapViewPath moveTo(MapViewPoint p) {
        super.moveTo(p);
        return this;
    }

    /**
     * Draw a line to the node.
     * @param n The node
     * @return this for easy chaining.
     */
    public MapViewPath lineTo(Node n) {
        lineTo(n.getEastNorth());
        return this;
    }

    /**
     * Draw a line to the position.
     * @param eastNorth The position
     * @return this for easy chaining.
     */
    public MapViewPath lineTo(EastNorth eastNorth) {
        lineTo(state.getPointFor(eastNorth));
        return this;
    }

    @Override
    public MapViewPath lineTo(MapViewPoint p) {
        super.lineTo(p);
        return this;
    }

    /**
     * Add the given shape centered around the current node.
     * @param p1 The point to draw around
     * @param symbol The symbol type
     * @param size The size of the symbol in pixel
     * @return this for easy chaining.
     */
    public MapViewPath shapeAround(Node p1, SymbolShape symbol, double size) {
        shapeAround(p1.getEastNorth(), symbol, size);
        return this;
    }

    /**
     * Add the given shape centered around the current position.
     * @param eastNorth The point to draw around
     * @param symbol The symbol type
     * @param size The size of the symbol in pixel
     * @return this for easy chaining.
     */
    public MapViewPath shapeAround(EastNorth eastNorth, SymbolShape symbol, double size) {
        shapeAround(state.getPointFor(eastNorth), symbol, size);
        return this;
    }

    @Override
    public MapViewPath shapeAround(MapViewPoint p, SymbolShape symbol, double size) {
        super.shapeAround(p, symbol, size);
        return this;
    }

    /**
     * Append a list of nodes
     * @param nodes The nodes to append
     * @param connect <code>true</code> if we should use a lineTo as first command.
     * @return this for easy chaining.
     */
    public MapViewPath append(Iterable<Node> nodes, boolean connect) {
        appendWay(nodes, connect, false);
        return this;
    }

    /**
     * Append a list of nodes as closed way.
     * @param nodes The nodes to append
     * @param connect <code>true</code> if we should use a lineTo as first command.
     * @return this for easy chaining.
     */
    public MapViewPath appendClosed(Iterable<Node> nodes, boolean connect) {
        appendWay(nodes, connect, true);
        return this;
    }

    private void appendWay(Iterable<Node> nodes, boolean connect, boolean close) {
        boolean useMoveTo = !connect;
        Node first = null;
        for (Node n : nodes) {
            if (useMoveTo) {
                moveTo(n);
            } else {
                lineTo(n);
            }
            if (close && first == null) {
                first = n;
            }
            useMoveTo = false;
        }
        if (first != null) {
            lineTo(first);
        }
    }
}
