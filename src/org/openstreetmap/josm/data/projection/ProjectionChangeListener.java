// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

@FunctionalInterface
public interface ProjectionChangeListener {
    void projectionChanged(Projection oldValue, Projection newValue);
}
