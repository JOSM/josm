// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This class implements the invalidation listener mechanism suggested by {@link MapViewPaintable}.
 *
 * @author Michael Zangl
 * @since 10031
 */
public abstract class AbstractMapViewPaintable implements MapViewPaintable {

    /**
     * A list of invalidation listeners to call when this layer is invalidated.
     */
    private final CopyOnWriteArrayList<PaintableInvalidationListener> invalidationListeners = new CopyOnWriteArrayList<>();

    /**
     * Adds a new paintable invalidation listener.
     * @param l The listener to add.
     */
    public void addInvalidationListener(PaintableInvalidationListener l) {
        invalidationListeners.add(l);
    }

    /**
     * Removes an added paintable invalidation listener.
     * @param l The listener to remove.
     */
    public void removeInvalidationListener(PaintableInvalidationListener l) {
        invalidationListeners.remove(l);
    }

    /**
     * This needs to be called whenever the content of this view was invalidated.
     */
    public void invalidate() {
        for (PaintableInvalidationListener l : invalidationListeners) {
            l.paintablInvalidated(new PaintableInvalidationEvent(this));
        }
    }
}
