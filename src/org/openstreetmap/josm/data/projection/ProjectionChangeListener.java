// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

/**
 * Interface for listeners to get notified when the (global) projection changes.
 */
@FunctionalInterface
public interface ProjectionChangeListener {
    void projectionChanged(Projection oldValue, Projection newValue);
}
