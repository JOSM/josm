// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Comparator;

/**
 * Provides some node order , based on coordinates, nodes with equal coordinates are equal.
 *
 * @author viesturs
 */
public class NodePositionComparator implements Comparator<Node> {
    @Override
    public int compare(Node n1, Node n2) {

        if (n1.getCoor().equalsEpsilon(n2.getCoor()))
            return 0;

        double dLat = n1.getCoor().lat() - n2.getCoor().lat();
        if (dLat > 0)
            return 1;
        if (dLat < 0)
            return -1;
        double dLon = n1.getCoor().lon() - n2.getCoor().lon();
        if (dLon == 0)
            return 0;
        return dLon > 0 ? 1 : -1;
    }
}
