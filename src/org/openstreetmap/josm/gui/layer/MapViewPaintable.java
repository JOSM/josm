// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import java.awt.Graphics2D;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.gui.MapView;

/**
 * This is a component that can be painted on the map view.
 * <p>
 * You might want to extend {@link AbstractMapViewPaintable} to ease implementation of this.
 * <p>
 * That class allows you to listen to paintable change events. Those methods may be moved here some time in the future.
 * @since   608 (creation)
 * @since 10600 (functional interface)
 */
@FunctionalInterface
public interface MapViewPaintable {

    /**
     * This event is fired whenever the paintable got invalidated and needs repainting some time in the future.
     * <p>
     * Note: We might add an area in the future.
     *
     * @author Michael Zangl
     */
    class PaintableInvalidationEvent {
        private final MapViewPaintable paintable;

        /**
         * Creates a new {@link PaintableInvalidationEvent}
         * @param paintable The paintable that is invalidated.
         */
        public PaintableInvalidationEvent(MapViewPaintable paintable) {
            this.paintable = paintable;
        }

        /**
         * Gets the layer that was invalidated.
         * @return The layer.
         */
        public MapViewPaintable getLayer() {
            return paintable;
        }

        @Override
        public String toString() {
            return "LayerInvalidationEvent [layer=" + paintable + ']';
        }
    }

    /**
     * This is a listener that listens to {@link PaintableInvalidationEvent}s
     * @author Michael Zangl
     * @since 10600 (functional interface)
     */
    @FunctionalInterface
    interface PaintableInvalidationListener {
        /**
         * Called whenever a {@link PaintableInvalidationEvent} is fired. This might be called from any thread.
         * @param event The event
         * @since 10600 (renamed)
         */
        void paintableInvalidated(PaintableInvalidationEvent event);
    }

    /**
     * Gets a new LayerPainter that paints this {@link MapViewPaintable} to the given map view.
     *
     * @author Michael Zangl
     * @since 10458
     */
    interface LayerPainter {

        /**
         * Paints the given layer.
         * <p>
         * This can be called in any thread at any time. You will not receive parallel calls for the same map view but you can receive parallel
         * calls if you use the same {@link LayerPainter} for different map views.
         * @param graphics The graphics object of the map view you should use.
         *                 It provides you with a content pane, the bounds and the view state.
         */
        void paint(MapViewGraphics graphics);

        /**
         * Called when the layer is removed from the map view and this painter is not used any more.
         * <p>
         * This method is called once on the painter returned by {@link Layer#attachToMapView}
         * @param event The event.
         */
        void detachFromMapView(MapViewEvent event);
    }

    /**
     * A event that is fired whenever the map view is attached or detached from any layer.
     * @author Michael Zangl
     * @see Layer#attachToMapView
     * @since 10458
     */
    class MapViewEvent {
        private final MapView mapView;
        private final boolean temporaryLayer;

        /**
         * Create a new {@link MapViewEvent}
         * @param mapView The map view
         * @param temporaryLayer <code>true</code> if this layer is in the temporary layer list of the view.
         */
        public MapViewEvent(MapView mapView, boolean temporaryLayer) {
            super();
            this.mapView = mapView;
            this.temporaryLayer = temporaryLayer;
        }

        /**
         * Gets the map view.
         * @return The map view.
         */
        public MapView getMapView() {
            return mapView;
        }

        /**
         * Determines if this {@link MapViewPaintable} is used as a temporary layer
         * @return true if this {@code MapViewPaintable} is used as a temporary layer.
         */
        public boolean isTemporaryLayer() {
            return temporaryLayer;
        }

        @Override
        public String toString() {
            return "AttachToMapViewEvent [mapView=" + mapView + ", temporaryLayer=" + temporaryLayer + "]";
        }
    }

    /**
     * Paint the dataset using the engine set.
     * @param g Graphics
     * @param mv The object that can translate GeoPoints to screen coordinates.
     * @param bbox Bounding box
     */
    void paint(Graphics2D g, MapView mv, Bounds bbox);

    /**
     * Adds a new paintable invalidation listener.
     * @param l The listener to add.
     * @since 12107
     */
    default void addInvalidationListener(PaintableInvalidationListener l) {
    }

    /**
     * Removes an added paintable invalidation listener. May throw an exception if the listener is added twice.
     * @param l The listener to remove.
     * @since 12107
     */
    default void removeInvalidationListener(PaintableInvalidationListener l) {
    }

}
