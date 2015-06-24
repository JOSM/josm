// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Point;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.Tile;
import org.openstreetmap.gui.jmapviewer.TileXY;
import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;
import org.openstreetmap.gui.jmapviewer.interfaces.TemplatedTileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.TMSTileSource;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.gui.layer.WMSLayer;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * Tile Source handling WMS providers
 *
 * @author Wiktor Niesiobędzki
 * @since 8526
 */
public class TemplatedWMSTileSource extends TMSTileSource implements TemplatedTileSource {
    private Map<String, String> headers = new HashMap<>();
    private List<String> serverProjections;
    private EastNorth topLeftCorner;

    private static final String COOKIE_HEADER   = "Cookie";
    private static final String PATTERN_HEADER  = "\\{header\\(([^,]+),([^}]+)\\)\\}";
    private static final String PATTERN_PROJ    = "\\{proj(\\([^})]+\\))?\\}";
    private static final String PATTERN_BBOX    = "\\{bbox\\}";
    private static final String PATTERN_W       = "\\{w\\}";
    private static final String PATTERN_S       = "\\{s\\}";
    private static final String PATTERN_E       = "\\{e\\}";
    private static final String PATTERN_N       = "\\{n\\}";
    private static final String PATTERN_WIDTH   = "\\{width\\}";
    private static final String PATTERN_HEIGHT  = "\\{height\\}";

    private static final NumberFormat latLonFormat = new DecimalFormat("###0.0000000", new DecimalFormatSymbols(Locale.US));

    private static final String[] ALL_PATTERNS = {
        PATTERN_HEADER, PATTERN_PROJ, PATTERN_BBOX, PATTERN_W, PATTERN_S, PATTERN_E, PATTERN_N, PATTERN_WIDTH, PATTERN_HEIGHT
    };

    /**
     * Creates a tile source based on imagery info
     * @param info imagery info
     */
    public TemplatedWMSTileSource(ImageryInfo info) {
        super(info);
        this.serverProjections = info.getServerProjections();
        handleTemplate();
        initProjection();
    }

    public void initProjection() {
        initProjection(Main.getProjection());
    }

    public void initProjection(Projection proj) {
        Bounds bounds = proj.getWorldBoundsLatLon();
        EastNorth min = proj.latlon2eastNorth(bounds.getMin());
        EastNorth max = proj.latlon2eastNorth(bounds.getMax());
        this.topLeftCorner = new EastNorth(min.east(), max.north());
    }

    @Override
    public int getDefaultTileSize() {
        return WMSLayer.PROP_IMAGE_SIZE.get();
    }

    // FIXME: remove in September 2015, when ImageryPreferenceEntry.tileSize will be initialized to -1 instead to 256
    // need to leave it as it is to keep compatiblity between tested and latest JOSM versions
    @Override
    public int getTileSize() {
        return WMSLayer.PROP_IMAGE_SIZE.get();
    }

    @Override
    public String getTileUrl(int zoom, int tilex, int tiley) throws IOException {
        String myProjCode = Main.getProjection().toCode();

        EastNorth nw = getTileEastNorth(tilex, tiley, zoom);
        EastNorth se = getTileEastNorth(tilex + 1, tiley + 1, zoom);

        double w = nw.getX();
        double n = nw.getY();

        double s = se.getY();
        double e = se.getX();

        if (!serverProjections.contains(myProjCode) && serverProjections.contains("EPSG:4326") && "EPSG:3857".equals(myProjCode)) {
            LatLon swll = Main.getProjection().eastNorth2latlon(new EastNorth(w, s));
            LatLon nell = Main.getProjection().eastNorth2latlon(new EastNorth(e, n));
            myProjCode = "EPSG:4326";
            s = swll.lat();
            w = swll.lon();
            n = nell.lat();
            e = nell.lon();
        }

        if ("EPSG:4326".equals(myProjCode) && !serverProjections.contains(myProjCode) && serverProjections.contains("CRS:84")) {
            myProjCode = "CRS:84";
        }

        // Bounding box coordinates have to be switched for WMS 1.3.0 EPSG:4326.
        //
        // Background:
        //
        // bbox=x_min,y_min,x_max,y_max
        //
        //      SRS=... is WMS 1.1.1
        //      CRS=... is WMS 1.3.0
        //
        // The difference:
        //      For SRS x is east-west and y is north-south
        //      For CRS x and y are as specified by the EPSG
        //          E.g. [1] lists lat as first coordinate axis and lot as second, so it is switched for EPSG:4326.
        //          For most other EPSG code there seems to be no difference.
        // [1] https://www.epsg-registry.org/report.htm?type=selection&entity=urn:ogc:def:crs:EPSG::4326&reportDetail=short&style=urn:uuid:report-style:default-with-code&style_name=OGP%20Default%20With%20Code&title=EPSG:4326
        boolean switchLatLon = false;
        if (baseUrl.toLowerCase().contains("crs=epsg:4326")) {
            switchLatLon = true;
        } else if (baseUrl.toLowerCase().contains("crs=") && "EPSG:4326".equals(myProjCode)) {
            switchLatLon = true;
        }
        String bbox;
        if (switchLatLon) {
            bbox = String.format("%s,%s,%s,%s", latLonFormat.format(s), latLonFormat.format(w), latLonFormat.format(n), latLonFormat.format(e));
        } else {
            bbox = String.format("%s,%s,%s,%s", latLonFormat.format(w), latLonFormat.format(s), latLonFormat.format(e), latLonFormat.format(n));
        }
        return baseUrl.
                replaceAll(PATTERN_PROJ,    myProjCode)
                .replaceAll(PATTERN_BBOX,   bbox)
                .replaceAll(PATTERN_W,      latLonFormat.format(w))
                .replaceAll(PATTERN_S,      latLonFormat.format(s))
                .replaceAll(PATTERN_E,      latLonFormat.format(e))
                .replaceAll(PATTERN_N,      latLonFormat.format(n))
                .replaceAll(PATTERN_WIDTH,  String.valueOf(getTileSize()))
                .replaceAll(PATTERN_HEIGHT, String.valueOf(getTileSize()))
                .replace(" ", "%20");
    }

    @Override
    public double getDistance(double lat1, double lon1, double lat2, double lon2) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public int lonToX(double lon, int zoom) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public int latToY(double lat, int zoom) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public double XToLon(int x, int zoom) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public double YToLat(int y, int zoom) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public double latToTileY(double lat, int zoom) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Coordinate tileXYToLatLon(Tile tile) {
        return tileXYToLatLon(tile.getXtile(), tile.getYtile(), tile.getZoom());
    }

    @Override
    public Coordinate tileXYToLatLon(TileXY xy, int zoom) {
        return tileXYToLatLon(xy.getXIndex(), xy.getYIndex(), zoom);
    }

    @Override
    public Coordinate tileXYToLatLon(int x, int y, int zoom) {
        LatLon ret = Main.getProjection().eastNorth2latlon(getTileEastNorth(x, y, zoom));
        return new Coordinate(ret.lat(),  ret.lon());
    }

    @Override
    public TileXY latLonToTileXY(double lat, double lon, int zoom) {
        Projection proj = Main.getProjection();
        EastNorth enPoint = proj.latlon2eastNorth(new LatLon(lat, lon));
        double scale = getDegreesPerTile(zoom);
        return new TileXY(
                (enPoint.east() - topLeftCorner.east()) / scale,
                (topLeftCorner.north() - enPoint.north()) / scale
                );
    }

    @Override
    public TileXY latLonToTileXY(ICoordinate point, int zoom) {
        return latLonToTileXY(point.getLat(),  point.getLon(), zoom);
    }

    @Override
    public int getTileXMax(int zoom) {
        return getTileXMax(zoom, Main.getProjection());
    }

    @Override
    public int getTileXMin(int zoom) {
        return 0;
    }

    //TODO: cache this method with projection code as the key
    @Override
    public int getTileYMax(int zoom) {
        return getTileYMax(zoom, Main.getProjection());
    }

    @Override
    public int getTileYMin(int zoom) {
        return 0;
    }

    @Override
    public Point latLonToXY(double lat, double lon, int zoom) {
        double scale = getDegreesPerTile(zoom) / getTileSize();
        EastNorth point = Main.getProjection().latlon2eastNorth(new LatLon(lat, lon));
        return new Point(
                    (int) Math.round((point.east() - topLeftCorner.east())   / scale),
                    (int) Math.round((topLeftCorner.north() - point.north()) / scale)
                );
    }

    @Override
    public Point latLonToXY(ICoordinate point, int zoom) {
        return latLonToXY(point.getLat(), point.getLon(), zoom);
    }

    @Override
    public Coordinate XYToLatLon(Point point, int zoom) {
        return XYToLatLon(point.x, point.y, zoom);
    }

    @Override
    public Coordinate XYToLatLon(int x, int y, int zoom) {
        double scale = getDegreesPerTile(zoom) / getTileSize();
        Projection proj = Main.getProjection();
        EastNorth ret = new EastNorth(
                topLeftCorner.east() + x * scale,
                topLeftCorner.north() - y * scale
                );
        LatLon ll = proj.eastNorth2latlon(ret);
        return new Coordinate(ll.lat(), ll.lon());
    }

    @Override
    public double lonToTileX(double lon, int zoom) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public double tileXToLon(int x, int zoom) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public double tileYToLat(int y, int zoom) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * Checks if url is acceptable by this Tile Source
     * @param url URL to check
     */
    public static void checkUrl(String url) {
        CheckParameterUtil.ensureParameterNotNull(url, "url");
        Matcher m = Pattern.compile("\\{[^}]*\\}").matcher(url);
        while (m.find()) {
            boolean isSupportedPattern = false;
            for (String pattern : ALL_PATTERNS) {
                if (m.group().matches(pattern)) {
                    isSupportedPattern = true;
                    break;
                }
            }
            if (!isSupportedPattern) {
                throw new IllegalArgumentException(
                        tr("{0} is not a valid WMS argument. Please check this server URL:\n{1}", m.group(), url));
            }
        }
    }

    private void handleTemplate() {
        // Capturing group pattern on switch values
        Pattern pattern = Pattern.compile(PATTERN_HEADER);
        StringBuffer output = new StringBuffer();
        Matcher matcher = pattern.matcher(this.baseUrl);
        while (matcher.find()) {
            headers.put(matcher.group(1), matcher.group(2));
            matcher.appendReplacement(output, "");
        }
        matcher.appendTail(output);
        this.baseUrl = output.toString();
    }

    protected EastNorth getTileEastNorth(int x, int y, int z) {
        double scale = getDegreesPerTile(z);
        return new EastNorth(
                        topLeftCorner.east() + x * scale,
                        topLeftCorner.north() - y * scale
                        );
    }

    private double getDegreesPerTile(int zoom) {
        return getDegreesPerTile(zoom, Main.getProjection());
    }

    private double getDegreesPerTile(int zoom, Projection proj) {
        Bounds bounds = proj.getWorldBoundsLatLon();
        EastNorth min = proj.latlon2eastNorth(bounds.getMin());
        EastNorth max = proj.latlon2eastNorth(bounds.getMax());
        int tilesPerZoom = (int) Math.pow(2, zoom);
        double ret = Math.max(
                Math.abs(max.getY() - min.getY()) / tilesPerZoom,
                Math.abs(max.getX() - min.getX()) / tilesPerZoom
                );

        return ret;
    }

    private int getTileYMax(int zoom, Projection proj) {
        double scale = getDegreesPerTile(zoom);
        Bounds bounds = Main.getProjection().getWorldBoundsLatLon();
        EastNorth min = proj.latlon2eastNorth(bounds.getMin());
        EastNorth max = proj.latlon2eastNorth(bounds.getMax());
        return (int) Math.ceil(Math.abs(max.getY() - min.getY()) / scale);
    }

    private int getTileXMax(int zoom, Projection proj) {
        double scale = getDegreesPerTile(zoom);
        Bounds bounds = Main.getProjection().getWorldBoundsLatLon();
        EastNorth min = proj.latlon2eastNorth(bounds.getMin());
        EastNorth max = proj.latlon2eastNorth(bounds.getMax());
        return (int) Math.ceil(Math.abs(max.getX() - min.getX()) / scale);
    }
}
