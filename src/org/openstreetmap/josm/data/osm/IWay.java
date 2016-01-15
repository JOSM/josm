// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

/**
 * IWay captures the common functions of {@link Way} and {@link WayData}.
 * @since 4098
 */
public interface IWay extends IPrimitive {

    /**
     * Replies the number of nodes in this way.
     *
     * @return the number of nodes in this way.
     */
    int getNodesCount();

    /**
     * Returns id of the node at given index.
     * @param idx node index
     * @return id of the node at given index
     */
    long getNodeId(int idx);

    /**
     * Determines if this way is closed.
     * @return {@code true} if this way is closed, {@code false} otherwise
     */
    boolean isClosed();
}
