// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor.paint;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.ILatLon;

/**
 * A record used for storing tile information for painting.
 * The origin is upper-left, not lower-left (so more like Google tile coordinates than TMS tile coordinates).
 * @since 19176
 */
public final class TileZXY implements ILatLon {
    private final int zoom;
    private final int x;
    private final int y;

    /**
     * Create a new {@link TileZXY} object
     * @param zoom The zoom for which this tile was created
     * @param x The x coordinate at the specified zoom level
     * @param y The y coordinate at the specified zoom level
     */
    public TileZXY(int zoom, int x, int y) {
        this.zoom = zoom;
        this.x = x;
        this.y = y;
    }

    /**
     * Get the zoom level
     * @return The zoom level for which this tile was created
     */
    public int zoom() {
        return this.zoom;
    }

    /**
     * Get the x coordinate
     * @return The x coordinate for this tile
     */
    @SuppressWarnings("PMD.ShortMethodName")
    public int x() {
        return this.x;
    }

    /**
     * Get the y coordinate
     * @return The y coordinate for this tile
     */
    @SuppressWarnings("PMD.ShortMethodName")
    public int y() {
        return this.y;
    }

    /**
     * Get the latitude for upper-left corner of this tile
     * @return The latitude
     */
    @Override
    public double lat() {
        return yToLat(this.y(), this.zoom());
    }

    /**
     * Get the longitude for the upper-left corner of this tile
     * @return The longitude
     */
    @Override
    public double lon() {
        return xToLon(this.x(), this.zoom());
    }

    /**
     * Convert a bounds to a series of tiles that entirely cover the bounds
     * @param minLat The minimum latitude
     * @param minLon The minimum longitude
     * @param maxLat The maximum latitude
     * @param maxLon The maximum longitude
     * @param zoom The zoom level to generate the tiles for
     * @return The stream of tiles
     */
    public static Stream<TileZXY> boundsToTiles(double minLat, double minLon, double maxLat, double maxLon, int zoom) {
        return boundsToTiles(minLat, minLon, maxLat, maxLon, zoom, 0);
    }

    /**
     * Convert a bounds to a series of tiles that entirely cover the bounds
     * @param minLat The minimum latitude
     * @param minLon The minimum longitude
     * @param maxLat The maximum latitude
     * @param maxLon The maximum longitude
     * @param zoom The zoom level to generate the tiles for
     * @param expansion The number of tiles to expand on the x/y axis (1 row north, 1 row south, 1 column left, 1 column right)
     * @return The stream of tiles
     */
    public static Stream<TileZXY> boundsToTiles(double minLat, double minLon, double maxLat, double maxLon, int zoom, int expansion) {
        final TileZXY upperRight = latLonToTile(maxLat, maxLon, zoom);
        final TileZXY lowerLeft = latLonToTile(minLat, minLon, zoom);
        return IntStream.rangeClosed(lowerLeft.x() - expansion, upperRight.x() + expansion)
                .mapToObj(x -> IntStream.rangeClosed(upperRight.y() - expansion, lowerLeft.y() + expansion)
                        .mapToObj(y -> new TileZXY(zoom, x, y)))
                .flatMap(stream -> stream);
    }

    /**
     * Convert a tile to the bounds for that tile
     * @param tile The tile to get the bounds for
     * @return The bounds
     */
    public static Bounds tileToBounds(TileZXY tile) {
        return new Bounds(yToLat(tile.y() + 1, tile.zoom()), xToLon(tile.x(), tile.zoom()),
                yToLat(tile.y(), tile.zoom()), xToLon(tile.x() + 1, tile.zoom()));
    }

    /**
     * Convert a x tile coordinate to a latitude
     * @param x The x coordinate
     * @param zoom The zoom level to use for the calculation
     * @return The latitude for the x coordinate (upper-left of the tile)
     */
    public static double xToLon(int x, int zoom) {
        return (x / Math.pow(2, zoom)) * 360 - 180;
    }

    /**
     * Convert a y tile coordinate to a latitude
     * @param y The y coordinate
     * @param zoom The zoom level to use for the calculation
     * @return The latitude for the y coordinate (upper-left of the tile)
     */
    public static double yToLat(int y, int zoom) {
        double t = Math.PI - (2 * Math.PI * y) / Math.pow(2, zoom);
        return 180 / Math.PI * Math.atan((Math.exp(t) - Math.exp(-t)) / 2);
    }

    /**
     * Convert a lat, lon, and zoom to a tile coordiante
     * @param lat The latitude
     * @param lon The longitude
     * @param zoom The zoom level
     * @return The specified tile coordinates at the specified zoom
     */
    public static TileZXY latLonToTile(double lat, double lon, int zoom) {
        final double zoom2 = Math.pow(2, zoom);
        final double latLog = Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat)));
        final int xCoord = (int) Math.floor(zoom2 * (180 + lon) / 360);
        final int yCoord = (int) Math.floor(zoom2 * (1 - latLog / Math.PI) / 2);
        return new TileZXY(zoom, xCoord, yCoord);
    }

    @Override
    public String toString() {
        return "TileZXY{" + zoom + "/" + x + "/" + y + "}";
    }

    @Override
    public int hashCode() {
        // We only care about comparing zoom, x, and y
        return Integer.hashCode(this.zoom) + 31 * (Integer.hashCode(this.x) + 31 * Integer.hashCode(this.y));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TileZXY) {
            TileZXY o = (TileZXY) obj;
            return this.zoom == o.zoom && this.x == o.x && this.y == o.y;
        }
        return false;
    }
}
