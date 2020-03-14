// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.List;

/**
 * IWay captures the common functions of {@link Way} and {@link WayData}.
 * @param <N> type of OSM node
 * @since 4098
 */
public interface IWay<N extends INode> extends IPrimitive {

    /**
     * Replies the number of nodes in this way.
     *
     * @return the number of nodes in this way.
     */
    int getNodesCount();

    /**
     * Determines if this way is empty, i.e. it has no nodes.
     * @return {@code true} if this way is empty, i.e. it has no nodes
     * @since 16119
     */
    default boolean isEmpty() {
        return getNodesCount() == 0;
    }

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
     * Replies the node at position <code>index</code>.
     *
     * @param index the position
     * @return  the node at position <code>index</code>
     * @throws ArrayIndexOutOfBoundsException if <code>index</code> &lt; 0
     * or <code>index</code> &gt;= {@link #getNodesCount()}
     * @since 1862
     * @since 13717 (IWay)
     */
    N getNode(int index);

    /**
     * Returns the list of nodes in this way.
     * @return the list of nodes in this way
     * @since 1862
     * @since 13717 (IWay)
     */
    List<N> getNodes();

    /**
     * Returns the list of node ids in this way.
     * @return the list of node ids in this way
     * @since 13717
     */
    List<Long> getNodeIds();

    /**
     * Returns id of the node at given index.
     * @param idx node index
     * @return id of the node at given index
     */
    long getNodeId(int idx);

    /**
     * Set new list of nodes to way. This method is preferred to multiple calls to addNode/removeNode
     * and similar methods because nodes are internally saved as array which means lower memory overhead
     * but also slower modifying operations.
     * @param nodes New way nodes. Can be null, in that case all way nodes are removed
     */
    void setNodes(List<N> nodes);

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

    /**
     * Returns the first node of this way.
     * The result equals {@link #getNode getNode}{@code (0)}.
     * @return the first node of this way
     * @since 13922
     */
    N firstNode();

    /**
     * Returns the last node of this way.
     * The result equals <code>{@link #getNode getNode}({@link #getNodesCount getNodesCount} - 1)</code>.
     * @return the last node of this way
     * @since 13922
     */
    N lastNode();

    /**
     * Replies true if the given node is the first or the last one of this way, false otherwise.
     * @param n The node to test
     * @return true if the {@code n} is the first or the last node, false otherwise.
     * @since 13922
     */
    boolean isFirstLastNode(INode n);

    /**
     * Replies true if the given node is an inner node of this way, false otherwise.
     * @param n The node to test
     * @return true if the {@code n} is an inner node, false otherwise.
     * @since 13922
     */
    boolean isInnerNode(INode n);
}
