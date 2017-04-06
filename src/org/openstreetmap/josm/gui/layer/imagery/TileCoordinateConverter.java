// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.imagery;

import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.openstreetmap.gui.jmapviewer.Tile;
import org.openstreetmap.gui.jmapviewer.TileXY;
import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;
import org.openstreetmap.gui.jmapviewer.interfaces.IProjected;
import org.openstreetmap.gui.jmapviewer.interfaces.TileSource;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
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
     */
    public TileCoordinateConverter(MapView mapView, TileSource tileSource, TileSourceDisplaySettings settings) {
        this.mapView = mapView;
        this.tileSource = tileSource;
        this.settings = settings;
    }

    private MapViewPoint pos(ICoordinate ll) {
        return mapView.getState().getPointFor(new LatLon(ll)).add(settings.getDisplacement());
    }

    private MapViewPoint pos(IProjected p) {
        return mapView.getState().getPointFor(new EastNorth(p)).add(settings.getDisplacement());
    }

    /**
     * Apply reverse shift to EastNorth coordinate.
     *
     * @param en EastNorth coordinate representing a pixel on screen
     * @return IProjected coordinate as it would e.g. be sent to a WMS server
     */
    public IProjected shiftDisplayToServer(EastNorth en) {
        return en.subtract(settings.getDisplacement()).toProjected();
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
        return this.getPixelForTile(tile.getXtile(), tile.getYtile(), tile.getZoom());
    }

    /**
     * Gets the position of the tile inside the map view.
     * @param tile The tile
     * @return The positon.
     */
    public Rectangle2D getRectangleForTile(Tile tile) {
        ICoordinate c1 = tile.getTileSource().tileXYToLatLon(tile);
        ICoordinate c2 = tile.getTileSource().tileXYToLatLon(tile.getXtile() + 1, tile.getYtile() + 1, tile.getZoom());

        return pos(c1).rectTo(pos(c2)).getInView();
    }

    /**
     * Returns a quadrilateral formed by the 4 corners of the tile in screen coordinates.
     *
     * If the tile is rectangular, this will be the exact border of the tile.
     * The tile may be more oddly shaped due to reprojection, then it is an approximation
     * of the tile outline.
     * @param tile the tile
     * @return quadrilateral tile outline in screen coordinates
     */
    public Shape getScreenQuadrilateralForTile(Tile tile) {
        Point2D p00 = this.getPixelForTile(tile.getXtile(), tile.getYtile(), tile.getZoom());
        Point2D p10 = this.getPixelForTile(tile.getXtile() + 1, tile.getYtile(), tile.getZoom());
        Point2D p11 = this.getPixelForTile(tile.getXtile() + 1, tile.getYtile() + 1, tile.getZoom());
        Point2D p01 = this.getPixelForTile(tile.getXtile(), tile.getYtile() + 1, tile.getZoom());

        Path2D pth = new Path2D.Double();
        pth.moveTo(p00.getX(), p00.getY());
        pth.lineTo(p01.getX(), p01.getY());
        pth.lineTo(p11.getX(), p11.getY());
        pth.lineTo(p10.getX(), p10.getY());
        pth.closePath();
        return pth;
    }

    /**
     * Returns average number of screen pixels per tile pixel for current mapview
     * @param zoom zoom level
     * @return average number of screen pixels per tile pixel
     */
    public double getScaleFactor(int zoom) {
        LatLon topLeft = mapView.getLatLon(0, 0);
        LatLon botRight = mapView.getLatLon(mapView.getWidth(), mapView.getHeight());
        TileXY t1 = tileSource.latLonToTileXY(topLeft.toCoordinate(), zoom);
        TileXY t2 = tileSource.latLonToTileXY(botRight.toCoordinate(), zoom);

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
        IProjected p1 = tileSource.tileXYtoProjected(tile.getXtile(), tile.getYtile(), tile.getZoom());
        IProjected p2 = tileSource.tileXYtoProjected(tile.getXtile() + 1, tile.getYtile() + 1, tile.getZoom());
        return new TileAnchor(pos(p1).getInView(), pos(p2).getInView());
    }
}
