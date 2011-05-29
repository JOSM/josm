// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

public interface IWay extends IPrimitive {

    int getNodesCount();
    long getNodeId(int idx);
    boolean isClosed();

}
