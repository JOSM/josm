// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Provides some node order , based on coordinates, nodes with equal coordinates are equal.
 *
 * @author viesturs
 */
public class NodePositionComparator implements Comparator<Node>, Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public int compare(Node n1, Node n2) {

        if (n1.equalsEpsilon(n2))
            return 0;

        int dLat = Double.compare(n1.lat(), n2.lat());
        return dLat != 0 ? dLat : Double.compare(n1.lon(), n2.lon());
    }
}
