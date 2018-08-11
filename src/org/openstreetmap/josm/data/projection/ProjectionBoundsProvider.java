// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import org.openstreetmap.josm.data.Bounds;

/**
 * Handles projection change events by keeping a clean state of current bounds.
 * @since 14120
 */
public interface ProjectionBoundsProvider {

    /**
     * Returns the bounds for the current projection. Used for projection events.
     * @return the bounds for the current projection
     * @see #restoreOldBounds
     */
    Bounds getRealBounds();

    /**
     * Restore clean state corresponding to old bounds after a projection change event.
     * @param oldBounds bounds previously returned by {@link #getRealBounds}, before the change of projection
     * @see #getRealBounds
     */
    void restoreOldBounds(Bounds oldBounds);
}
