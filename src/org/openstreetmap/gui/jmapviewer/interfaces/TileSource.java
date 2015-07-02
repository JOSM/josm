// License: GPL. For details, see Readme.txt file.
package org.openstreetmap.gui.jmapviewer.interfaces;

import java.awt.Point;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.Tile;
import org.openstreetmap.gui.jmapviewer.TileXY;

/**
 *
 * @author Jan Peter Stotz
 */
public interface TileSource extends Attributed {

    /**
     * Specifies the different mechanisms for detecting updated tiles
     * respectively only download newer tiles than those stored locally.
     *
     * <ul>
     * <li>{@link #IfNoneMatch} Server provides ETag header entry for all tiles
     * and <b>supports</b> conditional download via <code>If-None-Match</code>
     * header entry.</li>
     * <li>{@link #ETag} Server provides ETag header entry for all tiles but
     * <b>does not support</b> conditional download via
     * <code>If-None-Match</code> header entry.</li>
     * <li>{@link #IfModifiedSince} Server provides Last-Modified header entry
     * for all tiles and <b>supports</b> conditional download via
     * <code>If-Modified-Since</code> header entry.</li>
     * <li>{@link #LastModified} Server provides Last-Modified header entry for
     * all tiles but <b>does not support</b> conditional download via
     * <code>If-Modified-Since</code> header entry.</li>
     * <li>{@link #None} The server does not support any of the listed
     * mechanisms.</li>
     * </ul>
     *
     */
    @Deprecated //not used anymore
    public enum TileUpdate {
        IfNoneMatch, ETag, IfModifiedSince, LastModified, None
    }

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
     * @return The supported tile update mechanism
     * @see TileUpdate
     */
    TileUpdate getTileUpdate();

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
     * Specifies how large each tile is.
     * @return The size of a single tile in pixels. -1 if default size should be used
     */
    int getTileSize();

    /**
     * @return default tile size, for this tile source
     * TODO: @since
     */
    public int getDefaultTileSize();

    /**
     * Gets the distance using Spherical law of cosines.
     * @param la1 latitude of first point
     * @param lo1 longitude of first point
     * @param la2 latitude of second point
     * @param lo2 longitude of second point
     * @return the distance betwen first and second point, in m.
     */
    double getDistance(double la1, double lo1, double la2, double lo2);

    /**
     * Transform longitude to pixelspace.
     * @param aLongitude
     * @param aZoomlevel
     * @return [0..2^Zoomlevel*TILE_SIZE[
     */
    @Deprecated
    int lonToX(double aLongitude, int aZoomlevel);

    /**
     * Transforms latitude to pixelspace.
     * @param aLat
     * @param aZoomlevel
     * @return [0..2^Zoomlevel*TILE_SIZE[
     * @deprecated use lonLatToXY instead
     */
    @Deprecated
    int latToY(double aLat, int aZoomlevel);

    /**
     * @param lon
     * @param lat
     * @param zoom
     * @return transforms longitude and latitude to pixel space (as if all tiles at specified zoom level where joined)
     */
    public Point latLonToXY(double lat, double lon, int zoom);

    public Point latLonToXY(ICoordinate point, int zoom);

    /**
     * Transforms pixel coordinate X to longitude
     * @param aX
     * @param aZoomlevel
     * @return ]-180..180[
     */
    @Deprecated
    double XToLon(int aX, int aZoomlevel);

    /**
     * Transforms pixel coordinate Y to latitude.
     * @param aY
     * @param aZoomlevel
     * @return [MIN_LAT..MAX_LAT]
     */
    @Deprecated
    double YToLat(int aY, int aZoomlevel);

    /**
     * @param point
     * @param zoom
     * @return WGS84 Coordinates of given point
     */
    public ICoordinate XYToLatLon(Point point, int zoom);

    public ICoordinate XYToLatLon(int x, int y, int zoom);

    /**
     * Transforms longitude to X tile coordinate.
     * @param lon
     * @param zoom
     * @return [0..2^Zoomlevel[
     */
    @Deprecated
    double lonToTileX(double lon, int zoom);

    /**
     * Transforms latitude to Y tile coordinate.
     * @param lat
     * @param zoom
     * @return [0..2^Zoomlevel[
     */
    @Deprecated
    double latToTileY(double lat, int zoom);

    /**
     * @param lon
     * @param lat
     * @param zoom
     * @return x and y tile indices
     */
    public TileXY latLonToTileXY(double lat, double lon, int zoom);

    public TileXY latLonToTileXY(ICoordinate point, int zoom);

    /**
     * Transforms tile X coordinate to longitude.
     * @param x
     * @param zoom
     * @return ]-180..180[
     */
    @Deprecated
    double tileXToLon(int x, int zoom);

    /**
     * Transforms tile Y coordinate to latitude.
     * @param y
     * @param zoom
     * @return [MIN_LAT..MAX_LAT]
     */
    @Deprecated
    double tileYToLat(int y, int zoom);

    /**
     * @param xy
     * @param zoom
     * @return WGS84 coordinates of given tile
     */
    public ICoordinate tileXYToLatLon(TileXY xy, int zoom);

    public ICoordinate tileXYToLatLon(Tile tile);

    public ICoordinate tileXYToLatLon(int x, int y, int zoom);

    /**
     * @param zoom
     * @return maximum X index of tile for specified zoom level
     */
    public int getTileXMax(int zoom);

    /**
     *
     * @param zoom
     * @return minimum X index of tile for specified zoom level
     */
    public int getTileXMin(int zoom);

    /**
     *
     * @param zoom
     * @return maximum Y index of tile for specified zoom level
     */
    public int getTileYMax(int zoom);

    /**
     * @param zoom
     * @return minimum Y index of tile for specified zoom level
     */
    public int getTileYMin(int zoom);

    /**
     * Determines, if the returned data from TileSource represent "no tile at this zoom level" situation. Detection
     * algorithms differ per TileSource, so each TileSource should implement each own specific way.
     *
     * @param headers HTTP headers from response from TileSource server
     * @param statusCode HTTP status code
     * @param content byte array representing the data returned from the server
     * @return true, if "no tile at this zoom level" situation detected
     */
    public boolean isNoTileAtZoom(Map<String, List<String>> headers, int statusCode, byte[] content);

    /**
     * Extracts metadata about the tile based on HTTP headers
     *
     * @param headers HTTP headers from Tile Source server
     * @return tile metadata
     */
    public Map<String, String> getMetadata(Map<String, List<String>> headers);


}
