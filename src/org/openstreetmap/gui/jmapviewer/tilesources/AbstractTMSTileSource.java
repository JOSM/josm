// License: GPL. For details, see Readme.txt file.
package org.openstreetmap.gui.jmapviewer.tilesources;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.openstreetmap.gui.jmapviewer.OsmMercator;

public abstract class AbstractTMSTileSource extends AbstractTileSource {

    protected String name;
    protected String baseUrl;
    protected String id;
    private Map<String, String> noTileHeaders;

    public AbstractTMSTileSource(TileSourceInfo info) {
        this.name = info.getName();
        this.baseUrl = info.getUrl();
        if(baseUrl != null && baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0,baseUrl.length()-1);
        }
        this.id = info.getUrl();
        this.noTileHeaders = info.getNoTileHeaders();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public int getMaxZoom() {
        return 21;
    }

    @Override
    public int getMinZoom() {
        return 0;
    }

    public String getExtension() {
        return "png";
    }

    /**
     * @throws IOException when subclass cannot return the tile URL
     */
    public String getTilePath(int zoom, int tilex, int tiley) throws IOException {
        return "/" + zoom + "/" + tilex + "/" + tiley + "." + getExtension();
    }

    public String getBaseUrl() {
        return this.baseUrl;
    }

    @Override
    public String getTileUrl(int zoom, int tilex, int tiley) throws IOException {
        return this.getBaseUrl() + getTilePath(zoom, tilex, tiley);
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public String getTileType() {
        return "png";
    }

    /*
     * Most tilesources use OsmMercator projection.
     */
    @Override
    public int getTileSize() {
        return OsmMercator.TILE_SIZE;
    }

    @Override
    public double getDistance(double lat1, double lon1, double lat2, double lon2) {
        return OsmMercator.getDistance(lat1, lon1, lat2, lon2);
    }

    @Override
    public int LonToX(double lon, int zoom) {
        return (int )OsmMercator.LonToX(lon, zoom);
    }

    @Override
    public int LatToY(double lat, int zoom) {
        return (int )OsmMercator.LatToY(lat, zoom);
    }

    @Override
    public double XToLon(int x, int zoom) {
        return OsmMercator.XToLon(x, zoom);
    }

    @Override
    public double YToLat(int y, int zoom) {
        return OsmMercator.YToLat(y, zoom);
    }

    @Override
    public double latToTileY(double lat, int zoom) {
        return OsmMercator.LatToY(lat, zoom) / OsmMercator.TILE_SIZE;
    }

    @Override
    public double lonToTileX(double lon, int zoom) {
        return OsmMercator.LonToX(lon, zoom) / OsmMercator.TILE_SIZE;
    }

    @Override
    public double tileYToLat(int y, int zoom) {
        return OsmMercator.YToLat(y * OsmMercator.TILE_SIZE, zoom);
    }

    @Override
    public double tileXToLon(int x, int zoom) {
        return OsmMercator.XToLon(x * OsmMercator.TILE_SIZE, zoom);
    }

    @Override
    public boolean isNoTileAtZoom(Map<String, List<String>> headers, int statusCode, byte[] content) {
        if(noTileHeaders != null) {
            for (Entry<String, String> searchEntry: noTileHeaders.entrySet()) {
                List<String> headerVals = headers.get(searchEntry.getKey());
                if (headerVals != null) {
                    for (String headerValue: headerVals) {
                        if (headerValue.matches(searchEntry.getValue())) {
                            return true;
                        }
                    }
                }
            }
        }
        return super.isNoTileAtZoom(headers, statusCode, content);
    }
}
