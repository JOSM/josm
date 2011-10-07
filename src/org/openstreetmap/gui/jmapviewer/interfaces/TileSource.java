package org.openstreetmap.gui.jmapviewer.interfaces;

import java.awt.Image;
import java.io.IOException;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.JMapViewer;

//License: GPL. Copyright 2008 by Jan Peter Stotz

/**
 *
 * @author Jan Peter Stotz
 */
public interface TileSource {

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
     * A tile layer name has to be unique and has to consist only of characters
     * valid for filenames.
     *
     * @return Name of the tile layer
     */
    String getName();

    /**
     * Constructs the tile url.
     *
     * @param zoom
     * @param tilex
     * @param tiley
     * @return fully qualified url for downloading the specified tile image
     */
    String getTileUrl(int zoom, int tilex, int tiley) throws IOException;

    /**
     * Specifies the tile image type. For tiles rendered by Mapnik or
     * Osmarenderer this is usually <code>"png"</code>.
     *
     * @return file extension of the tile image type
     */
    String getTileType();

    /**
     * Specifies how large each tile is.
     * @return The size of a single tile in pixels.
     */
    int getTileSize();

    /**
     * @return True if the tile source requires attribution in text or image form.
     */
    boolean requiresAttribution();

    /**
     * @param zoom The optional zoom level for the view.
     * @param botRight The bottom right of the bounding box for attribution.
     * @param topLeft The top left of the bounding box for attribution.
     * @return Attribution text for the image source.
     */
    String getAttributionText(int zoom, Coordinate topLeft, Coordinate botRight);

    /**
     * @return The URL to open when the user clicks the attribution text.
     */
    String getAttributionLinkURL();

    /**
     * @return The URL for the attribution image. Null if no image should be displayed.
     */
    Image getAttributionImage();

    /**
     * @return The URL to open when the user clicks the attribution image.
     * When return value is null, the image is still displayed (provided getAttributionImage()
     * returns a value other than null), but the image does not link to a website.
     */
    String getAttributionImageURL();

    /**
     * @return The attribution "Terms of Use" text.
     * In case it returns null, but getTermsOfUseURL() is not null, a default
     * terms of use text is used.
     */
    String getTermsOfUseText();

    /**
     * @return The URL to open when the user clicks the attribution "Terms of Use" text.
     */
    String getTermsOfUseURL();

    double latToTileY(double lat, int zoom);

    double lonToTileX(double lon, int zoom);

    double tileYToLat(int y, int zoom);

    double tileXToLon(int x, int zoom);
}
