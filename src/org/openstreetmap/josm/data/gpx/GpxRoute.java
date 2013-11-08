// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import java.util.Collection;
import java.util.LinkedList;

public class GpxRoute extends WithAttributes {
    public Collection<WayPoint> routePoints = new LinkedList<WayPoint>();
}
