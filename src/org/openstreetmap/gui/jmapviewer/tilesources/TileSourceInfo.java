// License: GPL. For details, see LICENSE file.
package org.openstreetmap.gui.jmapviewer.tilesources;

import java.util.Map;

import org.openstreetmap.gui.jmapviewer.OsmMercator;

/**
 * Data class that keeps basic information about a tile source.
 *
 * @since 31122
 */
public class TileSourceInfo {
    /** id for this imagery entry, optional at the moment */
    protected String id;
    /** URL of the imagery service */
    protected  String url = null;

    /** name of the imagery layer */
    protected String name;

    /** headers meaning, that there is no tile at this zoom level */
    protected Map<String, String> noTileHeaders;

    /** minimum zoom level supported by the tile source */
    protected int minZoom;

    /** maximum zoom level supported by the tile source */
    protected int maxZoom;

    /** cookies that needs to be sent to tile source */
    protected String cookies;

    /** tile size of the displayed tiles */
    private int tileSize = OsmMercator.DEFAUL_TILE_SIZE; // FIXME: set to -1 for next release

    /** mapping &lt;header key, metadata key&gt; */
    protected Map<String, String> metadataHeaders;

    /**
     * Create a TileSourceInfo class
     *
     * @param name name
     * @param base_url base URL
     * @param id unique id
     */
    public TileSourceInfo(String name, String base_url, String id) {
        this(name);
        this.url = base_url;
        this.id = id;
    }

    /**
     * Create a TileSourceInfo class
     *
     * @param name name
     */
    public TileSourceInfo(String name) {
        this.name = name;
    }

    /**
     * Creates empty TileSourceInfo class
     */
    public TileSourceInfo() {
    }

    /**
     * Request name of the tile source
     * @return name of the tile source
     */
    public String getName() {
        return name;
    }

    /**
     * Request URL of the tile source
     * @return url of the tile source
     */
    public String getUrl() {
        return url;
    }

    /**
     * Request header information for empty tiles for servers delivering such tile types
     * @return map of headers, that when set, means that this is "no tile at this zoom level" situation
     */
    public Map<String, String> getNoTileHeaders() {
        return noTileHeaders;
    }

    /**
     * Request supported minimum zoom level
     * @return minimum zoom level supported by tile source
     */
    public int getMinZoom() {
        return minZoom;
    }

    /**
     * Request supported maximum zoom level
     * @return maximum zoom level supported by tile source
     */
    public int getMaxZoom() {
        return maxZoom;
    }

    /**
     * Request cookies to be sent together with request
     * @return cookies to be sent along with request to tile source
     */
    public String getCookies() {
        return cookies;
    }

    /**
     * Request tile size of this tile source
     * @return tile size provided by this tile source, or -1 when default value should be used
     */
    public int getTileSize() {
        return tileSize;
    }

    /**
     * Sets the tile size provided by this tile source
     * @param tileSize tile size in pixels
     */
    public void setTileSize(int tileSize) {
        if (tileSize == 0 || tileSize < -1) {
            throw new AssertionError("Invalid tile size: " + tileSize);
        }
        this.tileSize = tileSize;
    }

    /**
     *
     * @return mapping &lt;HTTP header name, Metadata key name&gt; for copying HTTP headers to Tile metadata
     * @since 31125
     */
    public Map<String, String> getMetadataHeaders() {
        return metadataHeaders;
    }
}
