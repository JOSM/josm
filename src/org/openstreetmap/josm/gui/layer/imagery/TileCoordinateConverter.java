// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.imagery;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.openstreetmap.gui.jmapviewer.Tile;
import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;
import org.openstreetmap.josm.data.coor.LatLon;
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

    /**
     * Create a new coordinate converter for the map view.
     * @param mapView The map view.
     * @param settings displacement settings.
     */
    public TileCoordinateConverter(MapView mapView, TileSourceDisplaySettings settings) {
        this.mapView = mapView;
        this.settings = settings;
    }

    private MapViewPoint pos(ICoordinate ll) {
        return mapView.getState().getPointFor(new LatLon(ll)).add(settings.getDisplacement());
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
}
