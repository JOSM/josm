// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Point;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.gui.jmapviewer.Tile;
import org.openstreetmap.gui.jmapviewer.TileXY;
import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;
import org.openstreetmap.gui.jmapviewer.interfaces.TemplatedTileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.TMSTileSource;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.ProjectionBounds;
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
    private final Map<String, String> headers = new ConcurrentHashMap<>();
    private final Set<String> serverProjections;
    private EastNorth anchorPosition;
    private int[] tileXMin;
    private int[] tileYMin;
    private int[] tileXMax;
    private int[] tileYMax;
    private double[] degreesPerTile;

    // CHECKSTYLE.OFF: SingleSpaceSeparator
    private static final Pattern PATTERN_HEADER = Pattern.compile("\\{header\\(([^,]+),([^}]+)\\)\\}");
    private static final Pattern PATTERN_PROJ   = Pattern.compile("\\{proj\\}");
    private static final Pattern PATTERN_WKID   = Pattern.compile("\\{wkid\\}");
    private static final Pattern PATTERN_BBOX   = Pattern.compile("\\{bbox\\}");
    private static final Pattern PATTERN_W      = Pattern.compile("\\{w\\}");
    private static final Pattern PATTERN_S      = Pattern.compile("\\{s\\}");
    private static final Pattern PATTERN_E      = Pattern.compile("\\{e\\}");
    private static final Pattern PATTERN_N      = Pattern.compile("\\{n\\}");
    private static final Pattern PATTERN_WIDTH  = Pattern.compile("\\{width\\}");
    private static final Pattern PATTERN_HEIGHT = Pattern.compile("\\{height\\}");
    private static final Pattern PATTERN_PARAM  = Pattern.compile("\\{([^}]+)\\}");
    // CHECKSTYLE.ON: SingleSpaceSeparator

    private static final NumberFormat latLonFormat = new DecimalFormat("###0.0000000", new DecimalFormatSymbols(Locale.US));

    private static final Pattern[] ALL_PATTERNS = {
        PATTERN_HEADER, PATTERN_PROJ, PATTERN_WKID, PATTERN_BBOX, PATTERN_W, PATTERN_S, PATTERN_E, PATTERN_N, PATTERN_WIDTH, PATTERN_HEIGHT
    };

    /*
     * Constant taken from OGC WMTS Implementation Specification (http://www.opengeospatial.org/standards/wmts)
     * From table E.4 - Definition of Well-known scale set GoogleMapsCompatibile
     *
     *  As higher zoom levels have denominator divided by 2, we keep only zoom level 1 in the code
     */
    private static final float SCALE_DENOMINATOR_ZOOM_LEVEL_1 = 559082264.0287178f;

    /**
     * Creates a tile source based on imagery info
     * @param info imagery info
     */
    public TemplatedWMSTileSource(ImageryInfo info) {
        super(info);
        this.serverProjections = new TreeSet<>(info.getServerProjections());
        handleTemplate();
        initProjection();
    }

    /**
     * Initializes class with current projection in JOSM. This call is needed every time projection changes.
     */
    public void initProjection() {
        initProjection(Main.getProjection());
    }

    private void initAnchorPosition(Projection proj) {
        Bounds worldBounds = proj.getWorldBoundsLatLon();
        EastNorth min = proj.latlon2eastNorth(worldBounds.getMin());
        EastNorth max = proj.latlon2eastNorth(worldBounds.getMax());
        this.anchorPosition = new EastNorth(min.east(), max.north());
    }

    /**
     * Initializes class with projection in JOSM. This call is needed every time projection changes.
     * @param proj new projection that shall be used for computations
     */
    public void initProjection(Projection proj) {
        initAnchorPosition(proj);
        ProjectionBounds worldBounds = proj.getWorldBoundsBoxEastNorth();

        EastNorth topLeft = new EastNorth(worldBounds.getMin().east(), worldBounds.getMax().north());
        EastNorth bottomRight = new EastNorth(worldBounds.getMax().east(), worldBounds.getMin().north());

        // use 256 as "tile size" to keep the scale in line with default tiles in Mercator projection
        double crsScale = 256 * 0.28e-03 / proj.getMetersPerUnit();
        tileXMin = new int[getMaxZoom() + 1];
        tileYMin = new int[getMaxZoom() + 1];
        tileXMax = new int[getMaxZoom() + 1];
        tileYMax = new int[getMaxZoom() + 1];
        degreesPerTile = new double[getMaxZoom() + 1];

        for (int zoom = 1; zoom <= getMaxZoom(); zoom++) {
            // use well known scale set "GoogleCompatibile" from OGC WMTS spec to calculate number of tiles per zoom level
            // this makes the zoom levels "glued" to standard TMS zoom levels
            degreesPerTile[zoom] = (SCALE_DENOMINATOR_ZOOM_LEVEL_1 / Math.pow(2d, zoom - 1d)) * crsScale;
            TileXY minTileIndex = eastNorthToTileXY(topLeft, zoom);
            tileXMin[zoom] = minTileIndex.getXIndex();
            tileYMin[zoom] = minTileIndex.getYIndex();
            TileXY maxTileIndex = eastNorthToTileXY(bottomRight, zoom);
            tileXMax[zoom] = maxTileIndex.getXIndex();
            tileYMax[zoom] = maxTileIndex.getYIndex();
        }
    }

    @Override
    public int getDefaultTileSize() {
        return WMSLayer.PROP_IMAGE_SIZE.get();
    }

    @Override
    public String getTileUrl(int zoom, int tilex, int tiley) {
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
        // CHECKSTYLE.OFF: LineLength
        // [1] https://www.epsg-registry.org/report.htm?type=selection&entity=urn:ogc:def:crs:EPSG::4326&reportDetail=short&style=urn:uuid:report-style:default-with-code&style_name=OGP%20Default%20With%20Code&title=EPSG:4326
        // CHECKSTYLE.ON: LineLength
        boolean switchLatLon = false;
        if (baseUrl.toLowerCase(Locale.US).contains("crs=epsg:4326")) {
            switchLatLon = true;
        } else if (baseUrl.toLowerCase(Locale.US).contains("crs=")) {
            // assume WMS 1.3.0
            switchLatLon = Main.getProjection().switchXY();
        }
        String bbox;
        if (switchLatLon) {
            bbox = String.format("%s,%s,%s,%s", latLonFormat.format(s), latLonFormat.format(w), latLonFormat.format(n), latLonFormat.format(e));
        } else {
            bbox = String.format("%s,%s,%s,%s", latLonFormat.format(w), latLonFormat.format(s), latLonFormat.format(e), latLonFormat.format(n));
        }

        // Using StringBuffer and generic PATTERN_PARAM matcher gives 2x performance improvement over replaceAll
        StringBuffer url = new StringBuffer(baseUrl.length());
        Matcher matcher = PATTERN_PARAM.matcher(baseUrl);
        while (matcher.find()) {
            String replacement;
            switch (matcher.group(1)) {
            case "proj":
                replacement = myProjCode;
                break;
            case "wkid":
                replacement = myProjCode.startsWith("EPSG:") ? myProjCode.substring(5) : myProjCode;
                break;
            case "bbox":
                replacement = bbox;
                break;
            case "w":
                replacement = latLonFormat.format(w);
                break;
            case "s":
                replacement = latLonFormat.format(s);
                break;
            case "e":
                replacement = latLonFormat.format(e);
                break;
            case "n":
                replacement = latLonFormat.format(n);
                break;
            case "width":
            case "height":
                replacement = String.valueOf(getTileSize());
                break;
            default:
                replacement = '{' + matcher.group(1) + '}';
            }
            matcher.appendReplacement(url, replacement);
        }
        matcher.appendTail(url);
        return url.toString().replace(" ", "%20");
    }

    @Override
    public String getTileId(int zoom, int tilex, int tiley) {
        return getTileUrl(zoom, tilex, tiley);
    }

    @Override
    public ICoordinate tileXYToLatLon(Tile tile) {
        return tileXYToLatLon(tile.getXtile(), tile.getYtile(), tile.getZoom());
    }

    @Override
    public ICoordinate tileXYToLatLon(TileXY xy, int zoom) {
        return tileXYToLatLon(xy.getXIndex(), xy.getYIndex(), zoom);
    }

    @Override
    public ICoordinate tileXYToLatLon(int x, int y, int zoom) {
        return Main.getProjection().eastNorth2latlon(getTileEastNorth(x, y, zoom)).toCoordinate();
    }

    @Override
    public TileXY latLonToTileXY(double lat, double lon, int zoom) {
        Projection proj = Main.getProjection();
        EastNorth enPoint = proj.latlon2eastNorth(new LatLon(lat, lon));
        return eastNorthToTileXY(enPoint, zoom);
    }

    private TileXY eastNorthToTileXY(EastNorth enPoint, int zoom) {
        double scale = getDegreesPerTile(zoom);
        return new TileXY(
                (enPoint.east() - anchorPosition.east()) / scale,
                (anchorPosition.north() - enPoint.north()) / scale
                );
    }

    @Override
    public TileXY latLonToTileXY(ICoordinate point, int zoom) {
        return latLonToTileXY(point.getLat(), point.getLon(), zoom);
    }

    @Override
    public int getTileXMax(int zoom) {
        return tileXMax[zoom];
    }

    @Override
    public int getTileXMin(int zoom) {
        return tileXMin[zoom];
    }

    @Override
    public int getTileYMax(int zoom) {
        return tileYMax[zoom];
    }

    @Override
    public int getTileYMin(int zoom) {
        return tileYMin[zoom];
    }

    @Override
    public Point latLonToXY(double lat, double lon, int zoom) {
        double scale = getDegreesPerTile(zoom) / getTileSize();
        EastNorth point = Main.getProjection().latlon2eastNorth(new LatLon(lat, lon));
        return new Point(
                    (int) Math.round((point.east() - anchorPosition.east()) / scale),
                    (int) Math.round((anchorPosition.north() - point.north()) / scale)
                );
    }

    @Override
    public Point latLonToXY(ICoordinate point, int zoom) {
        return latLonToXY(point.getLat(), point.getLon(), zoom);
    }

    @Override
    public ICoordinate xyToLatLon(Point point, int zoom) {
        return xyToLatLon(point.x, point.y, zoom);
    }

    @Override
    public ICoordinate xyToLatLon(int x, int y, int zoom) {
        double scale = getDegreesPerTile(zoom) / getTileSize();
        Projection proj = Main.getProjection();
        EastNorth ret = new EastNorth(
                anchorPosition.east() + x * scale,
                anchorPosition.north() - y * scale
                );
        return proj.eastNorth2latlon(ret).toCoordinate();
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
        Matcher m = PATTERN_PARAM.matcher(url);
        while (m.find()) {
            boolean isSupportedPattern = false;
            for (Pattern pattern : ALL_PATTERNS) {
                if (pattern.matcher(m.group()).matches()) {
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
        StringBuffer output = new StringBuffer();
        Matcher matcher = PATTERN_HEADER.matcher(this.baseUrl);
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
                        anchorPosition.east() + x * scale,
                        anchorPosition.north() - y * scale
                        );
    }

    private double getDegreesPerTile(int zoom) {
        return degreesPerTile[zoom];
    }
}
