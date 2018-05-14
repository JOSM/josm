// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.openstreetmap.josm.tools.JosmRuntimeException;

/**
 * Stores primitives in quad buckets. This can be used to hold a collection of primitives, e.g. in a {@link DataSet}
 *
 * This class does not do any synchronization.
 * @author Michael Zangl
 * @param <N> type representing OSM nodes
 * @param <W> type representing OSM ways
 * @param <R> type representing OSM relations
 * @since 12048
 */
public class QuadBucketPrimitiveStore<N extends INode, W extends IWay<N>, R extends IRelation<?>> {
    /**
     * All nodes goes here, even when included in other data (ways etc). This enables the instant
     * conversion of the whole DataSet by iterating over this data structure.
     */
    private final QuadBuckets<N> nodes = new QuadBuckets<>();

    /**
     * All ways (Streets etc.) in the DataSet.
     *
     * The way nodes are stored only in the way list.
     */
    private final QuadBuckets<W> ways = new QuadBuckets<>();

    /**
     * All relations/relationships
     */
    private final Collection<R> relations = new ArrayList<>();

    /**
     * Constructs a new {@code QuadBucketPrimitiveStore}.
     */
    public QuadBucketPrimitiveStore() {
    }

    /**
     * Searches for nodes in the given bounding box.
     * @param bbox the bounding box
     * @return List of nodes in the given bbox. Can be empty but not null
     */
    public List<N> searchNodes(BBox bbox) {
        return nodes.search(bbox);
    }

    /**
     * Determines if the given node can be retrieved in the store through its bounding box. Useful for dataset consistency test.
     * @param n The node to search
     * @return {@code true} if {@code n} can be retrieved in this store, {@code false} otherwise
     */
    public boolean containsNode(N n) {
        return nodes.contains(n);
    }

    /**
     * Searches for ways in the given bounding box.
     * @param bbox the bounding box
     * @return List of ways in the given bbox. Can be empty but not null
     */
    public List<W> searchWays(BBox bbox) {
        return ways.search(bbox);
    }

    /**
     * Determines if the given way can be retrieved in the store through its bounding box. Useful for dataset consistency test.
     * @param w The way to search
     * @return {@code true} if {@code w} can be retrieved in this store, {@code false} otherwise
     */
    public boolean containsWay(W w) {
        return ways.contains(w);
    }

    /**
     * Searches for relations in the given bounding box.
     * @param bbox the bounding box
     * @return List of relations in the given bbox. Can be empty but not null
     */
    public List<R> searchRelations(BBox bbox) {
        // QuadBuckets might be useful here (don't forget to do reindexing after some of rm is changed)
        return relations.stream()
                .filter(r -> r.getBBox().intersects(bbox))
                .collect(Collectors.toList());
    }

    /**
     * Determines if the given relation can be retrieved in the store through its bounding box. Useful for dataset consistency test.
     * @param r The relation to search
     * @return {@code true} if {@code r} can be retrieved in this store, {@code false} otherwise
     */
    public boolean containsRelation(R r) {
        return relations.contains(r);
    }

    /**
     * Adds a primitive to this quad bucket store
     *
     * @param primitive the primitive.
     */
    @SuppressWarnings("unchecked")
    public void addPrimitive(IPrimitive primitive) {
        boolean success = false;
        if (primitive instanceof INode) {
            success = nodes.add((N) primitive);
        } else if (primitive instanceof IWay) {
            success = ways.add((W) primitive);
        } else if (primitive instanceof IRelation) {
            success = relations.add((R) primitive);
        }
        if (!success) {
            throw new JosmRuntimeException("failed to add primitive: "+primitive);
        }
    }

    protected void removePrimitive(IPrimitive primitive) {
        boolean success = false;
        if (primitive instanceof INode) {
            success = nodes.remove(primitive);
        } else if (primitive instanceof IWay) {
            success = ways.remove(primitive);
        } else if (primitive instanceof IRelation) {
            success = relations.remove(primitive);
        }
        if (!success) {
            throw new JosmRuntimeException("failed to remove primitive: "+primitive);
        }
    }

    /**
     * Re-index the node after it's position was changed.
     * @param node The node to re-index
     * @param nUpdater update node position
     * @param wUpdater update way position
     * @param rUpdater update relation position
     */
    @SuppressWarnings("unchecked")
    protected void reindexNode(N node, Consumer<N> nUpdater, Consumer<W> wUpdater, Consumer<R> rUpdater) {
        if (!nodes.remove(node))
            throw new JosmRuntimeException("Reindexing node failed to remove");
        nUpdater.accept(node);
        if (!nodes.add(node))
            throw new JosmRuntimeException("Reindexing node failed to add");
        for (IPrimitive primitive: node.getReferrers()) {
            if (primitive instanceof IWay) {
                reindexWay((W) primitive, wUpdater, rUpdater);
            } else {
                reindexRelation((R) primitive, rUpdater);
            }
        }
    }

    /**
     * Re-index the way after it's position was changed.
     * @param way The way to re-index
     * @param wUpdater update way position
     * @param rUpdater update relation position
     */
    @SuppressWarnings("unchecked")
    protected void reindexWay(W way, Consumer<W> wUpdater, Consumer<R> rUpdater) {
        BBox before = way.getBBox();
        if (!ways.remove(way))
            throw new JosmRuntimeException("Reindexing way failed to remove");
        wUpdater.accept(way);
        if (!ways.add(way))
            throw new JosmRuntimeException("Reindexing way failed to add");
        if (!way.getBBox().equals(before)) {
            for (IPrimitive primitive: way.getReferrers()) {
                reindexRelation((R) primitive, rUpdater);
            }
        }
    }

    /**
     * Re-index the relation after it's position was changed.
     * @param relation The relation to re-index
     * @param rUpdater update relation position
     */
    @SuppressWarnings("unchecked")
    protected void reindexRelation(R relation, Consumer<R> rUpdater) {
        BBox before = relation.getBBox();
        rUpdater.accept(relation);
        if (!before.equals(relation.getBBox())) {
            for (IPrimitive primitive: relation.getReferrers()) {
                reindexRelation((R) primitive, rUpdater);
            }
        }
    }

    /**
     * Removes all primitives from the this store.
     */
    public void clear() {
        nodes.clear();
        ways.clear();
        relations.clear();
    }
}
