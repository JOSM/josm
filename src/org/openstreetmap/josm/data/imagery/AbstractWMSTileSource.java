// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import java.awt.Point;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import org.openstreetmap.gui.jmapviewer.Projected;
import org.openstreetmap.gui.jmapviewer.Tile;
import org.openstreetmap.gui.jmapviewer.TileXY;
import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;
import org.openstreetmap.gui.jmapviewer.interfaces.IProjected;
import org.openstreetmap.gui.jmapviewer.tilesources.TMSTileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.TileSourceInfo;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.Projection;

/**
 * Base class for different WMS tile sources those based on URL templates and those based on WMS endpoints
 * @author Wiktor Niesiobędzki
 * @since 10990
 */
public abstract class AbstractWMSTileSource extends TMSTileSource {

    static final NumberFormat LATLON_FORMAT = new DecimalFormat("###0.0000000", new DecimalFormatSymbols(Locale.US));

    private EastNorth anchorPosition;
    private int[] tileXMin;
    private int[] tileYMin;
    private int[] tileXMax;
    private int[] tileYMax;
    private double[] degreesPerTile;
    private static final double SCALE_DENOMINATOR_ZOOM_LEVEL_1 = 5.59082264028718e08;
    private Projection tileProjection;

    /**
     * Constructs a new {@code AbstractWMSTileSource}.
     * @param info tile source info
     * @param tileProjection the tile projection
     */
    protected AbstractWMSTileSource(TileSourceInfo info, Projection tileProjection) {
        super(info);
        this.tileProjection = tileProjection;
    }

    private void initAnchorPosition(Projection proj) {
        Bounds worldBounds = proj.getWorldBoundsLatLon();
        EastNorth min = proj.latlon2eastNorth(worldBounds.getMin());
        EastNorth max = proj.latlon2eastNorth(worldBounds.getMax());
        this.anchorPosition = new EastNorth(min.east(), max.north());
    }

    public void setTileProjection(Projection tileProjection) {
        this.tileProjection = tileProjection;
        initProjection();
    }

    public Projection getTileProjection() {
        return this.tileProjection;
    }

    /**
     * Initializes class with current projection in JOSM. This call is needed every time projection changes.
     */
    public void initProjection() {
        initProjection(this.tileProjection);
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
            // use well known scale set "GoogleCompatible" from OGC WMTS spec to calculate number of tiles per zoom level
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
    public ICoordinate tileXYToLatLon(Tile tile) {
        return tileXYToLatLon(tile.getXtile(), tile.getYtile(), tile.getZoom());
    }

    @Override
    public ICoordinate tileXYToLatLon(TileXY xy, int zoom) {
        return tileXYToLatLon(xy.getXIndex(), xy.getYIndex(), zoom);
    }

    @Override
    public ICoordinate tileXYToLatLon(int x, int y, int zoom) {
        return CoordinateConversion.llToCoor(tileProjection.eastNorth2latlon(getTileEastNorth(x, y, zoom)));
    }

    private TileXY eastNorthToTileXY(EastNorth enPoint, int zoom) {
        double scale = getDegreesPerTile(zoom);
        return new TileXY(
                (enPoint.east() - anchorPosition.east()) / scale,
                (anchorPosition.north() - enPoint.north()) / scale
                );
    }

    @Override
    public TileXY latLonToTileXY(double lat, double lon, int zoom) {
        EastNorth enPoint = tileProjection.latlon2eastNorth(new LatLon(lat, lon));
        return eastNorthToTileXY(enPoint, zoom);
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
        EastNorth point = tileProjection.latlon2eastNorth(new LatLon(lat, lon));
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
        EastNorth ret = new EastNorth(
                anchorPosition.east() + x * scale,
                anchorPosition.north() - y * scale
                );
        return CoordinateConversion.llToCoor(tileProjection.eastNorth2latlon(ret));
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

    @Override
    public IProjected tileXYtoProjected(int x, int y, int zoom) {
        EastNorth en = getTileEastNorth(x, y, zoom);
        return new Projected(en.east(), en.north());
    }

    @Override
    public TileXY projectedToTileXY(IProjected p, int zoom) {
        return eastNorthToTileXY(new EastNorth(p.getEast(), p.getNorth()), zoom);
    }

    @Override
    public String getServerCRS() {
        return this.tileProjection.toCode();
    }

    protected String getBbox(int zoom, int tilex, int tiley, boolean switchLatLon) {
        EastNorth nw = getTileEastNorth(tilex, tiley, zoom);
        EastNorth se = getTileEastNorth(tilex + 1, tiley + 1, zoom);

        double w = nw.getX();
        double n = nw.getY();

        double s = se.getY();
        double e = se.getX();

        return switchLatLon ?
                getBboxstr(s, w, n, e)
                : getBboxstr(w, s, e, n);
    }

    private static String getBboxstr(double x1, double x2, double x3, double x4) {
        return new StringBuilder(64)
                .append(LATLON_FORMAT.format(x1))
                .append(',')
                .append(LATLON_FORMAT.format(x2))
                .append(',')
                .append(LATLON_FORMAT.format(x3))
                .append(',')
                .append(LATLON_FORMAT.format(x4))
                .toString();
    }
}
