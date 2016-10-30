// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.imagery;

import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.openstreetmap.gui.jmapviewer.TileXY;

/**
 * This is a rectangular range of tiles.
 */
public class TileRange {
    protected int minX;
    protected int maxX;
    protected int minY;
    protected int maxY;
    protected int zoom;

    protected TileRange() {
    }

    protected TileRange(TileXY t1, TileXY t2, int zoom) {
        minX = (int) Math.floor(Math.min(t1.getX(), t2.getX()));
        minY = (int) Math.floor(Math.min(t1.getY(), t2.getY()));
        maxX = (int) Math.ceil(Math.max(t1.getX(), t2.getX()));
        maxY = (int) Math.ceil(Math.max(t1.getY(), t2.getY()));
        this.zoom = zoom;
    }

    protected double tilesSpanned() {
        return Math.sqrt(1.0 * this.size());
    }

    /**
     * Returns size
     * @return size
     */
    public int size() {
        int xSpan = maxX - minX + 1;
        int ySpan = maxY - minY + 1;
        return xSpan * ySpan;
    }

    /**
     * Gets a stream of all tile positions in this set
     * @return A stream of all positions
     */
    public Stream<TilePosition> tilePositions() {
        if (zoom == 0) {
            return Stream.empty();
        } else {
            return IntStream.rangeClosed(minX, maxX).mapToObj(
                    x -> IntStream.rangeClosed(minY, maxY).mapToObj(y -> new TilePosition(x, y, zoom))
                    ).flatMap(Function.identity());
        }
    }
}
