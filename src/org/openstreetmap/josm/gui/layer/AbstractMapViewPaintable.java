// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This class implements the invalidation listener mechanism suggested by {@link MapViewPaintable} and a default #atta
 *
 * @author Michael Zangl
 * @since 10031
 */
public abstract class AbstractMapViewPaintable implements MapViewPaintable {

    /**
     * This is the default implementation of the layer painter.
     * <p>
     * You should not use it. Write your own implementation and put your paint code into that class.
     * <p>
     * It propagates all calls to the
     * {@link MapViewPaintable#paint(java.awt.Graphics2D, org.openstreetmap.josm.gui.MapView, org.openstreetmap.josm.data.Bounds)} method.
     * @author Michael Zangl
     * @since 10458
     */
    protected class CompatibilityModeLayerPainter implements LayerPainter {
        @Override
        public void paint(MapViewGraphics graphics) {
            AbstractMapViewPaintable.this.paint(
                    graphics.getDefaultGraphics(),
                    graphics.getMapView(),
                    graphics.getClipBounds().getLatLonBoundsBox());
        }

        @Override
        public void detachFromMapView(MapViewEvent event) {
            // ignored in old implementation
        }
    }

    /**
     * A list of invalidation listeners to call when this layer is invalidated.
     */
    private final CopyOnWriteArrayList<PaintableInvalidationListener> invalidationListeners = new CopyOnWriteArrayList<>();

    /**
     * This method is called whenever this layer is added to a map view.
     * <p>
     * You need to return a painter here.
     * The {@link MapViewPaintable.LayerPainter#detachFromMapView} method is called when the layer is removed
     * from that map view. You are free to reuse painters.
     * <p>
     * You should always call the super method. See {@link #createMapViewPainter} if you want to influence painter creation.
     * <p>
     * This replaces {@link Layer#hookUpMapView} in the long run.
     * @param event the event.
     * @return A layer painter.
     * @since 10458
     */
    public LayerPainter attachToMapView(MapViewEvent event) {
        return createMapViewPainter(event);
    }

    /**
     * Creates a new LayerPainter.
     * @param event The event that triggered the creation.
     * @return The painter.
     * @since 10458
     */
    protected LayerPainter createMapViewPainter(MapViewEvent event) {
        return new CompatibilityModeLayerPainter();
    }

    @Override
    public void addInvalidationListener(PaintableInvalidationListener l) {
        invalidationListeners.add(l);
    }

    @Override
    public void removeInvalidationListener(PaintableInvalidationListener l) {
        invalidationListeners.remove(l);
    }

    /**
     * This needs to be called whenever the content of this view was invalidated.
     * It triggers a repaint of the components that display this layer.
     */
    public void invalidate() {
        PaintableInvalidationEvent event = new PaintableInvalidationEvent(this);
        for (PaintableInvalidationListener l : invalidationListeners) {
            l.paintableInvalidated(event);
        }
    }
}
