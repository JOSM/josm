// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.imagery;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Objects;

import org.openstreetmap.gui.jmapviewer.Tile;
import org.openstreetmap.gui.jmapviewer.TileXY;
import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;
import org.openstreetmap.gui.jmapviewer.interfaces.IProjected;
import org.openstreetmap.gui.jmapviewer.interfaces.TileSource;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.imagery.CoordinateConversion;
import org.openstreetmap.josm.data.projection.Projecting;
import org.openstreetmap.josm.data.projection.ShiftedProjecting;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapViewState.MapViewPoint;

/**
 * This class handles tile coordinate management and computes their position in the map view.
 * @author Michael Zangl
 * @since 10651
 */
public class TileCoordinateConverter {
    private final MapView mapView;
    private final TileSourceDisplaySettings settings;
    private final TileSource tileSource;

    /**
     * Create a new coordinate converter for the map view.
     * @param mapView The map view.
     * @param tileSource The tile source to use when converting coordinates.
     * @param settings displacement settings.
     * @throws NullPointerException if one argument is null
     */
    public TileCoordinateConverter(MapView mapView, TileSource tileSource, TileSourceDisplaySettings settings) {
        this.mapView = Objects.requireNonNull(mapView, "mapView");
        this.tileSource = Objects.requireNonNull(tileSource, "tileSource");
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    private MapViewPoint pos(ICoordinate ll) {
        return mapView.getState().getPointFor(CoordinateConversion.coorToLL(ll)).add(settings.getDisplacement());
    }

    private MapViewPoint pos(IProjected p) {
        return mapView.getState().getPointFor(CoordinateConversion.projToEn(p)).add(settings.getDisplacement());
    }

    /**
     * Apply reverse shift to EastNorth coordinate.
     *
     * @param en EastNorth coordinate representing a pixel on screen
     * @return IProjected coordinate as it would e.g. be sent to a WMS server
     */
    public IProjected shiftDisplayToServer(EastNorth en) {
        return CoordinateConversion.enToProj(en.subtract(settings.getDisplacement()));
    }

    /**
     * Gets the projecting instance to use to convert between latlon and eastnorth coordinates.
     * @return The {@link Projecting} instance.
     */
    public Projecting getProjecting() {
        return new ShiftedProjecting(mapView.getProjection(), settings.getDisplacement());
    }

    /**
     * Gets the top left position of the tile inside the map view.
     * @param x x tile index
     * @param y y tile index
     * @param zoom zoom level
     * @return the position
     */
    public Point2D getPixelForTile(int x, int y, int zoom) {
        ICoordinate coord = tileSource.tileXYToLatLon(x, y, zoom);
        return pos(coord).getInView();
    }

    /**
     * Gets the top left position of the tile inside the map view.
     * @param tile The tile
     * @return The position.
     */
    public Point2D getPixelForTile(Tile tile) {
        return getPixelForTile(tile.getXtile(), tile.getYtile(), tile.getZoom());
    }

    /**
     * Convert screen pixel coordinate to tile position at certain zoom level.
     * @param sx x coordinate (screen pixel)
     * @param sy y coordinate (screen pixel)
     * @param zoom zoom level
     * @return the tile
     */
    public TileXY getTileforPixel(int sx, int sy, int zoom) {
        if (requiresReprojection()) {
            LatLon ll = getProjecting().eastNorth2latlonClamped(mapView.getEastNorth(sx, sy));
            return tileSource.latLonToTileXY(CoordinateConversion.llToCoor(ll), zoom);
        } else {
            IProjected p = shiftDisplayToServer(mapView.getEastNorth(sx, sy));
            return tileSource.projectedToTileXY(p, zoom);
        }
    }

    /**
     * Gets the position of the tile inside the map view.
     * @param tile The tile
     * @return The positon as a rectangle in screen coordinates
     */
    public Rectangle2D getRectangleForTile(Tile tile) {
        ICoordinate c1 = tile.getTileSource().tileXYToLatLon(tile);
        ICoordinate c2 = tile.getTileSource().tileXYToLatLon(tile.getXtile() + 1, tile.getYtile() + 1, tile.getZoom());

        return pos(c1).rectTo(pos(c2)).getInView();
    }

    /**
     * Returns a shape that approximates the outline of the tile in screen coordinates.
     *
     * If the tile is rectangular, this will be the exact border of the tile.
     * The tile may be more oddly shaped due to reprojection, then it is an approximation
     * of the tile outline.
     * @param tile the tile
     * @return tile outline in screen coordinates
     */
    public Shape getTileShapeScreen(Tile tile) {
        if (requiresReprojection()) {
            Point2D p00 = this.getPixelForTile(tile.getXtile(), tile.getYtile(), tile.getZoom());
            Point2D p10 = this.getPixelForTile(tile.getXtile() + 1, tile.getYtile(), tile.getZoom());
            Point2D p11 = this.getPixelForTile(tile.getXtile() + 1, tile.getYtile() + 1, tile.getZoom());
            Point2D p01 = this.getPixelForTile(tile.getXtile(), tile.getYtile() + 1, tile.getZoom());
            return new Polygon(new int[] {
                    (int) Math.round(p00.getX()),
                    (int) Math.round(p01.getX()),
                    (int) Math.round(p11.getX()),
                    (int) Math.round(p10.getX())},
                new int[] {
                    (int) Math.round(p00.getY()),
                    (int) Math.round(p01.getY()),
                    (int) Math.round(p11.getY()),
                    (int) Math.round(p10.getY())}, 4);
        } else {
            Point2D p00 = this.getPixelForTile(tile.getXtile(), tile.getYtile(), tile.getZoom());
            Point2D p11 = this.getPixelForTile(tile.getXtile() + 1, tile.getYtile() + 1, tile.getZoom());
            return new Rectangle((int) Math.round(p00.getX()), (int) Math.round(p00.getY()),
                    (int) Math.round(p11.getX()) - (int) Math.round(p00.getX()),
                    (int) Math.round(p11.getY()) - (int) Math.round(p00.getY()));
        }
    }

    /**
     * Returns average number of screen pixels per tile pixel for current mapview
     * @param zoom zoom level
     * @return average number of screen pixels per tile pixel
     */
    public double getScaleFactor(int zoom) {
        TileXY t1, t2;
        if (requiresReprojection()) {
            LatLon topLeft = mapView.getLatLon(0, 0);
            LatLon botRight = mapView.getLatLon(mapView.getWidth(), mapView.getHeight());
            t1 = tileSource.latLonToTileXY(CoordinateConversion.llToCoor(topLeft), zoom);
            t2 = tileSource.latLonToTileXY(CoordinateConversion.llToCoor(botRight), zoom);
        } else {
            EastNorth topLeftEN = mapView.getEastNorth(0, 0);
            EastNorth botRightEN = mapView.getEastNorth(mapView.getWidth(), mapView.getHeight());
            t1 = tileSource.projectedToTileXY(CoordinateConversion.enToProj(topLeftEN), zoom);
            t2 = tileSource.projectedToTileXY(CoordinateConversion.enToProj(botRightEN), zoom);
        }
        int screenPixels = mapView.getWidth()*mapView.getHeight();
        double tilePixels = Math.abs((t2.getY()-t1.getY())*(t2.getX()-t1.getX())*tileSource.getTileSize()*tileSource.getTileSize());
        if (screenPixels == 0 || tilePixels == 0) return 1;
        return screenPixels/tilePixels;
    }

    /**
     * Get {@link TileAnchor} for a tile in screen pixel coordinates.
     * @param tile the tile
     * @return position of the tile in screen coordinates
     */
    public TileAnchor getScreenAnchorForTile(Tile tile) {
        if (requiresReprojection()) {
            ICoordinate c1 = tile.getTileSource().tileXYToLatLon(tile);
            ICoordinate c2 = tile.getTileSource().tileXYToLatLon(tile.getXtile() + 1, tile.getYtile() + 1, tile.getZoom());
            return new TileAnchor(pos(c1).getInView(), pos(c2).getInView());
        } else {
            IProjected p1 = tileSource.tileXYtoProjected(tile.getXtile(), tile.getYtile(), tile.getZoom());
            IProjected p2 = tileSource.tileXYtoProjected(tile.getXtile() + 1, tile.getYtile() + 1, tile.getZoom());
            return new TileAnchor(pos(p1).getInView(), pos(p2).getInView());
        }
    }

    /**
     * Return true if tiles need to be reprojected from server projection to display projection.
     * @return true if tiles need to be reprojected from server projection to display projection
     */
    public boolean requiresReprojection() {
        return !Objects.equals(tileSource.getServerCRS(), Main.getProjection().toCode());
    }
}
