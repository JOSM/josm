// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.ArrayList;
import java.util.List;

public class WayData extends PrimitiveData {

    private final List<Long> nodes = new ArrayList<Long>();

    public List<Long> getNodes() {
        return nodes;
    }

}
