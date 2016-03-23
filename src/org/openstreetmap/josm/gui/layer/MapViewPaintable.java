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
 */
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
            super();
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
            return "LayerInvalidationEvent [layer=" + paintable + "]";
        }
    }

    /**
     * This is a listener that listens to {@link PaintableInvalidationEvent}s
     * @author Michael Zangl
     */
    interface PaintableInvalidationListener {
        /**
         * Called whenever a {@link PaintableInvalidationEvent} is fired. This might be called from any thread.
         * @param event The event
         */
        void paintablInvalidated(PaintableInvalidationEvent event);
    }

    /**
     * Paint the dataset using the engine set.
     * @param g Graphics
     * @param mv The object that can translate GeoPoints to screen coordinates.
     * @param bbox Bounding box
     */
    void paint(Graphics2D g, MapView mv, Bounds bbox);
}
