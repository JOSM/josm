// License: GPL. For details, see Readme.txt file.
package org.openstreetmap.gui.jmapviewer.tilesources;

import java.awt.Point;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.OsmMercator;
import org.openstreetmap.gui.jmapviewer.Tile;
import org.openstreetmap.gui.jmapviewer.TileXY;
import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;

/**
 * Class generalizing all tile based tile sources
 *
 * @author Wiktor Niesiobędzki
 *
 */
public abstract class AbstractTMSTileSource extends AbstractTileSource {

    protected String name;
    protected String baseUrl;
    protected String id;
    private Map<String, String> noTileHeaders;
    private Map<String, String> metadataHeaders;
    protected int tileSize;
    protected OsmMercator osmMercator;

    /**
     * Creates an instance based on TileSource information
     *
     * @param info description of the Tile Source
     */
    public AbstractTMSTileSource(TileSourceInfo info) {
        this.name = info.getName();
        this.baseUrl = info.getUrl();
        if(baseUrl != null && baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0,baseUrl.length()-1);
        }
        this.id = info.getUrl();
        this.noTileHeaders = info.getNoTileHeaders();
        this.metadataHeaders = info.getMetadataHeaders();
        this.tileSize = info.getTileSize();
        osmMercator = new OsmMercator(this.tileSize);
    }

    /**
     * @return default tile size to use, when not set in Imagery Preferences
     */
    public int getDefaultTileSize() {
        return OsmMercator.DEFAUL_TILE_SIZE;
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

    /**
     * @return image extension, used for URL creation
     */
    public String getExtension() {
        return "png";
    }

    /**
     * @param zoom level of the tile
     * @param tilex tile number in x axis
     * @param tiley tile number in y axis
     * @return String containg path part of URL of the tile
     * @throws IOException when subclass cannot return the tile URL
     */
    public String getTilePath(int zoom, int tilex, int tiley) throws IOException {
        return "/" + zoom + "/" + tilex + "/" + tiley + "." + getExtension();
    }

    /**
     * @return Base part of the URL of the tile source
     */
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

    /*
     * Most tilesources use OsmMercator projection.
     */
    @Override
    public int getTileSize() {
        if (tileSize <= 0) {
            return getDefaultTileSize();
        }
        return tileSize;
    }

    @Override
    public double getDistance(double lat1, double lon1, double lat2, double lon2) {
        return osmMercator.getDistance(lat1, lon1, lat2, lon2);
    }

    @Override
    public int lonToX(double lon, int zoom) {
        return (int )osmMercator.LonToX(lon, zoom);
    }

    @Override
    public int latToY(double lat, int zoom) {
        return (int )osmMercator.LatToY(lat, zoom);
    }

    @Override
    public Point latLonToXY(double lat, double lon, int zoom) {
        return new Point(
                (int)osmMercator.LonToX(lon, zoom),
                (int)osmMercator.LatToY(lat, zoom)
                );
    }

    @Override
    public Point latLonToXY(ICoordinate point, int zoom) {
        return latLonToXY(point.getLat(), point.getLon(), zoom);
    }

    @Override
    public double XToLon(int x, int zoom) {
        return osmMercator.XToLon(x, zoom);
    }

    @Override
    public double YToLat(int y, int zoom) {
        return osmMercator.YToLat(y, zoom);
    }

    @Override
    public Coordinate XYToLatLon(Point point, int zoom) {
        return XYToLatLon(point.x, point.y, zoom);
    }

    @Override
    public Coordinate XYToLatLon(int x, int y, int zoom) {
        return new Coordinate(
                osmMercator.YToLat(y, zoom),
                osmMercator.XToLon(x, zoom)
                );
    }

    @Override
    public double latToTileY(double lat, int zoom) {
        return osmMercator.LatToY(lat, zoom) / tileSize;
    }

    @Override
    public double lonToTileX(double lon, int zoom) {
        return osmMercator.LonToX(lon, zoom) / tileSize;
    }

    @Override
    public TileXY latLonToTileXY(double lat, double lon, int zoom) {
        return new TileXY(
                osmMercator.LonToX(lon, zoom) / tileSize,
                osmMercator.LatToY(lat, zoom) / tileSize
                );
    }

    @Override
    public TileXY latLonToTileXY(ICoordinate point, int zoom) {
        return latLonToTileXY(point.getLat(), point.getLon(), zoom);
    }

    @Override
    public double tileYToLat(int y, int zoom) {
        return osmMercator.YToLat(y * tileSize, zoom);
    }

    @Override
    public double tileXToLon(int x, int zoom) {
        return osmMercator.XToLon(x * tileSize, zoom);
    }

    @Override
    public Coordinate tileXYToLatLon(TileXY xy, int zoom) {
        return tileXYToLatLon(xy.getXIndex(), xy.getYIndex(), zoom);
    }

    @Override
    public Coordinate tileXYToLatLon(Tile tile) {
        return tileXYToLatLon(tile.getXtile(), tile.getYtile(), tile.getZoom());
    }

    @Override
    public Coordinate tileXYToLatLon(int x, int y, int zoom) {
        return new Coordinate(
                osmMercator.YToLat(y * tileSize, zoom),
                osmMercator.XToLon(x * tileSize, zoom)
                );
    }

    @Override
    public int getTileXMax(int zoom) {
        return getTileMax(zoom);
    }

    @Override
    public int getTileXMin(int zoom) {
        return 0;
    }

    @Override
    public int getTileYMax(int zoom) {
        return getTileMax(zoom);
    }

    @Override
    public int getTileYMin(int zoom) {
        return 0;
    }


    @Override
    public boolean isNoTileAtZoom(Map<String, List<String>> headers, int statusCode, byte[] content) {
        if (noTileHeaders != null && headers != null) {
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

    @Override
    public Map<String, String> getMetadata(Map<String, List<String>> headers) {
        Map<String, String> ret = new HashMap<>();
        if (metadataHeaders != null && headers != null) {
            for (Entry<String, String> searchEntry: metadataHeaders.entrySet()) {
                List<String> headerVals = headers.get(searchEntry.getKey());
                if (headerVals != null) {
                    for (String headerValue: headerVals) {
                        ret.put(searchEntry.getValue(), headerValue);
                    }
                }
            }
        }
        return ret;
    }

    private int getTileMax(int zoom) {
        return (int)Math.pow(2.0, zoom) - 1;
    }
}
