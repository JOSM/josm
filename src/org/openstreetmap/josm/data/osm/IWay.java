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
     * Replies the real number of nodes in this way (full number of nodes minus one if this way is closed)
     *
     * @return the real number of nodes in this way.
     *
     * @see #getNodesCount()
     * @see #isClosed()
     * @since 5847
     * @since 13564 (IWay)
     */
    default int getRealNodesCount() {
        int count = getNodesCount();
        return isClosed() ? count-1 : count;
    }

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

    @Override
    default int compareTo(IPrimitive o) {
        if (o instanceof IRelation)
            return 1;
        return o instanceof IWay ? Long.compare(getUniqueId(), o.getUniqueId()) : -1;
    }

    @Override
    default String getDisplayName(NameFormatter formatter) {
        return formatter.format(this);
    }
}
