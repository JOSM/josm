// License: GPL. For details, see Readme.txt file.
package org.openstreetmap.gui.jmapviewer.interfaces;

import java.awt.Point;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.Tile;
import org.openstreetmap.gui.jmapviewer.TileRange;
import org.openstreetmap.gui.jmapviewer.TileXY;

/**
 *
 * @author Jan Peter Stotz
 */
public interface TileSource extends Attributed {

    /**
     * Specifies the maximum zoom value. The number of zoom levels is [0..
     * {@link #getMaxZoom()}].
     *
     * @return maximum zoom value that has to be smaller or equal to
     *         {@link JMapViewer#MAX_ZOOM}
     */
    int getMaxZoom();

    /**
     * Specifies the minimum zoom value. This value is usually 0.
     * Only for maps that cover a certain region up to a limited zoom level
     * this method should return a value different than 0.
     *
     * @return minimum zoom value - usually 0
     */
    int getMinZoom();

    /**
     * A tile layer name as displayed to the user.
     *
     * @return Name of the tile layer
     */
    String getName();

    /**
     * A unique id for this tile source.
     *
     * Unlike the name it has to be unique and has to consist only of characters
     * valid for filenames.
     *
     * @return the id
     */
    String getId();

    /**
     * Constructs the tile url.
     *
     * @param zoom zoom level
     * @param tilex X coordinate
     * @param tiley Y coordinate
     * @return fully qualified url for downloading the specified tile image
     * @throws IOException if any I/O error occurs
     */
    String getTileUrl(int zoom, int tilex, int tiley) throws IOException;

    /**
     * Creates tile identifier that is unique among all tile sources, but the same tile will always
     * get the same identifier. Used for creation of cache key.
     *
     * @param zoom zoom level
     * @param tilex X coordinate
     * @param tiley Y coordinate
     * @return tile identifier
     */
    String getTileId(int zoom, int tilex, int tiley);

    /**
     * Specifies how large each tile is.
     * @return The size of a single tile in pixels. -1 if default size should be used
     */
    int getTileSize();

    /**
     * @return default tile size, for this tile source
     */
    int getDefaultTileSize();

    /**
     * Gets the distance using Spherical law of cosines.
     * @param la1 latitude of first point
     * @param lo1 longitude of first point
     * @param la2 latitude of second point
     * @param lo2 longitude of second point
     * @return the distance between first and second point, in m.
     */
    double getDistance(double la1, double lo1, double la2, double lo2);

    /**
     * Transforms longitude and latitude to pixel space (as if all tiles at specified zoom level where joined).
     * @param lon longitude
     * @param lat latitude
     * @param zoom zoom level
     * @return the pixel coordinates
     */
    Point latLonToXY(double lat, double lon, int zoom);

    /**
     * Transforms longitude and latitude to pixel space (as if all tiles at specified zoom level where joined).
     * @param point point
     * @param zoom zoom level
     * @return the pixel coordinates
     */
    Point latLonToXY(ICoordinate point, int zoom);

    /**
     * Transforms a point in pixel space to longitude/latitude (WGS84).
     * @param point point
     * @param zoom zoom level
     * @return WGS84 Coordinates of given point
     */
    ICoordinate xyToLatLon(Point point, int zoom);

    /**
     * Transforms a point in pixel space to longitude/latitude (WGS84).
     * @param x X coordinate
     * @param y Y coordinate
     * @param zoom zoom level
     * @return WGS84 Coordinates of given point
     */
    ICoordinate xyToLatLon(int x, int y, int zoom);

    /**
     * Transforms longitude and latitude to tile indices.
     * @param lon longitude
     * @param lat latitude
     * @param zoom zoom level
     * @return x and y tile indices
     */
    TileXY latLonToTileXY(double lat, double lon, int zoom);

    /**
     * Transforms longitude and latitude to tile indices.
     * @param point point
     * @param zoom zoom level
     * @return x and y tile indices
     */
    TileXY latLonToTileXY(ICoordinate point, int zoom);

    /**
     * Transforms tile indices to longitude and latitude.
     * @param xy X/Y tile indices
     * @param zoom zoom level
     * @return WGS84 coordinates of given tile
     */
    ICoordinate tileXYToLatLon(TileXY xy, int zoom);

    /**
     * Determines to longitude and latitude of a tile.
     * (Refers to the tile origin - upper left tile corner)
     * @param tile Tile
     * @return WGS84 coordinates of given tile
     */
    ICoordinate tileXYToLatLon(Tile tile);

    /**
     * Transforms tile indices to longitude and latitude.
     * @param x x tile index
     * @param y y tile index
     * @param zoom zoom level
     * @return WGS84 coordinates of given tile
     */
    ICoordinate tileXYToLatLon(int x, int y, int zoom);

    /**
     * Get maximum x index of tile for specified zoom level.
     * @param zoom zoom level
     * @return maximum x index of tile for specified zoom level
     */
    int getTileXMax(int zoom);

    /**
     * Get minimum x index of tile for specified zoom level.
     * @param zoom zoom level
     * @return minimum x index of tile for specified zoom level
     */
    int getTileXMin(int zoom);

    /**
     * Get maximum y index of tile for specified zoom level.
     * @param zoom zoom level
     * @return maximum y index of tile for specified zoom level
     */
    int getTileYMax(int zoom);

    /**
     * Get minimum y index of tile for specified zoom level
     * @param zoom zoom level
     * @return minimum y index of tile for specified zoom level
     */
    int getTileYMin(int zoom);

    /**
     * Determines, if the returned data from TileSource represent "no tile at this zoom level" situation. Detection
     * algorithms differ per TileSource, so each TileSource should implement each own specific way.
     *
     * @param headers HTTP headers from response from TileSource server
     * @param statusCode HTTP status code
     * @param content byte array representing the data returned from the server
     * @return true, if "no tile at this zoom level" situation detected
     */
    boolean isNoTileAtZoom(Map<String, List<String>> headers, int statusCode, byte[] content);

    /**
     * Extracts metadata about the tile based on HTTP headers
     *
     * @param headers HTTP headers from Tile Source server
     * @return tile metadata
     */
    Map<String, String> getMetadata(Map<String, List<String>> headers);

    /**
     * Convert tile indices (x/y/zoom) into projected coordinates of the tile origin.
     * @param x x tile index
     * @param y z tile index
     * @param zoom zoom level
     * @return projected coordinates of the tile origin
     */
    IProjected tileXYtoProjected(int x, int y, int zoom);

    /**
     * Convert projected coordinates to tile indices.
     * @param p projected coordinates
     * @param zoom zoom level
     * @return corresponding tile index x/y (floating point, truncate to integer
     * for tile index)
     */
    TileXY projectedToTileXY(IProjected p, int zoom);

    /**
     * Check if one tile is inside another tile.
     * @param inner the tile that is suspected to be inside the other tile
     * @param outer the tile that might contain the first tile
     * @return true if first tile is inside second tile (or both are identical),
     * false otherwise
     */
    boolean isInside(Tile inner, Tile outer);

    /**
     * Returns a range of tiles, that cover a given tile, which is
     * usually at a different zoom level.
     *
     * In standard tile layout, 4 tiles cover a tile one zoom lower, 16 tiles
     * cover a tile 2 zoom levels below etc.
     * If the zoom level of the covering tiles is greater or equal, a single
     * tile suffices.
     *
     * @param tile the tile to cover
     * @param newZoom zoom level of the covering tiles
     * @return TileRange of all covering tiles at zoom <code>newZoom</code>
     */
    TileRange getCoveringTileRange(Tile tile, int newZoom);

    /**
     * Get coordinate reference system for this tile source.
     *
     * E.g. "EPSG:3857" for Google-Mercator.
     * @return code for the coordinate reference system in use
     */
    String getServerCRS();

    /**
     * Determines if this imagery supports "/dirty" mode (tile re-rendering).
     * @return <code>true</code> if it supports "/dirty" mode (tile re-rendering)
     */
    default boolean isModTileFeatures() {
        return false;
    }
}
