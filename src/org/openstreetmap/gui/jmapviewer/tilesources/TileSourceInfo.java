// License: GPL. For details, see LICENSE file.
package org.openstreetmap.gui.jmapviewer.tilesources;

import java.util.Map;

/**
 * Data class that keeps basic information about a tile source.
 */
public class TileSourceInfo {
    /** id for this imagery entry, optional at the moment */
    protected String id;
    /** URL of the imagery service */
    protected  String url = null;

    /** name of the imagery layer */
    protected String name;

    /** headers meaning, that there is no tile at this zoom level */
    protected Map<String, String> notileHeaders;

    /** minimum zoom level supported by the tile source */
    protected int minZoom;

    /** maximum zoom level supported by the tile source */
    protected int maxZoom;

    /** cookies that needs to be sent to tile source */
    protected String cookies;


    /**
     * Create a TileSourceInfo class
     *
     * @param name
     * @param base_url
     * @param id
     */
    public TileSourceInfo(String name, String base_url, String id) {
        this(name);
        this.url = base_url;
        this.id = id;
    }

    /**
     * Create a TileSourceInfo class
     *
     * @param name
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
     *
     * @return name of the tile source
     */
    public String getName() {
        return name;
    }

    /**
     *
     * @return url of the tile source
     */
    public String getUrl() {
        return url;
    }

    /**
     *
     * @return map of headers, that when set, means that this is "no tile at this zoom level" situation
     */
    public Map<String, String> getNoTileHeaders() {
        return notileHeaders;
    }

    /**
     *
     * @return minimum zoom level supported by tile source
     */
    public int getMinZoom() {
        return minZoom;
    }

    /**
     *
     * @return maximum zoom level supported by tile source
     */
    public int getMaxZoom() {
        return maxZoom;
    }

    /**
     *
     * @return cookies to be sent along with request to tile source
     */
    public String getCookies() {
        return cookies;
    }

}
