// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import java.util.Collection;
import java.util.LinkedList;

public class GpxRoute extends WithAttributes {
    public Collection<WayPoint> routePoints = new LinkedList<>();

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + ((routePoints == null) ? 0 : routePoints.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        GpxRoute other = (GpxRoute) obj;
        if (routePoints == null) {
            if (other.routePoints != null)
                return false;
        } else if (!routePoints.equals(other.routePoints))
            return false;
        return true;
    }
}
