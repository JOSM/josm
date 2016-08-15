// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.imagery;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.openstreetmap.gui.jmapviewer.Tile;
import org.openstreetmap.gui.jmapviewer.TileXY;
import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;
import org.openstreetmap.gui.jmapviewer.interfaces.TileSource;
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
    private MapView mapView;
    private TileSourceDisplaySettings settings;
    private TileSource tileSource;

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

    /**
     * Gets the projecting instance to use to convert between latlon and eastnorth coordinates.
     * @return The {@link Projecting} instance.
     */
    public Projecting getProjecting() {
        return new ShiftedProjecting(mapView.getProjection(), settings.getDisplacement());
    }

    /**
     * Gets the top left position of the tile inside the map view.
     * @param tile The tile
     * @return The positon.
     */
    public Point2D getPixelForTile(Tile tile) {
        ICoordinate coord = tile.getTileSource().tileXYToLatLon(tile);
        return pos(coord).getInView();
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
}
