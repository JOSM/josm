// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import java.awt.Graphics2D;

import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapViewState.MapViewRectangle;

/**
 * This class provides layers with access to drawing on the map view.
 * <p>
 * It contains information about the state of the map view.
 * <p>
 * In the future, it may add support for parallel drawing or layer caching.
 * <p>
 * It is intended to be used during {@link MapView#paint(java.awt.Graphics)}
 * @author Michael Zangl
 * @since 10458
 */
public class MapViewGraphics {

    private final Graphics2D graphics;
    private final MapView mapView;
    private final MapViewRectangle clipBounds;

    /**
     * Constructs a new {@code MapViewGraphics}.
     * @param mapView map view
     * @param graphics default graphics
     * @param clipBounds clip bounds for this graphics instance
     */
    public MapViewGraphics(MapView mapView, Graphics2D graphics, MapViewRectangle clipBounds) {
        this.mapView = mapView;
        this.graphics = graphics;
        this.clipBounds = clipBounds;
    }

    /**
     * Gets the {@link Graphics2D} you should use to paint on this graphics object. It may already have some data painted on it.
     * You should paint your layer data on this graphics.
     * @return The {@link Graphics2D} instance.
     */
    public Graphics2D getDefaultGraphics() {
        return graphics;
    }

    /**
     * Gets the {@link MapView} that is the base to this draw call.
     * @return The map view.
     */
    public MapView getMapView() {
        return mapView;
    }

    /**
     * Gets the clip bounds for this graphics instance.
     * @return The clip bounds.
     */
    public MapViewRectangle getClipBounds() {
        return clipBounds;
    }

    @Override
    public String toString() {
        return "MapViewGraphics [graphics=" + graphics + ", mapView=" + mapView + ", clipBounds=" + clipBounds + ']';
    }
}
