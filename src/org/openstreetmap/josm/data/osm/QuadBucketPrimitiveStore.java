// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.tools.JosmRuntimeException;

/**
 * Stores primitives in quad buckets. This can be used to hold a collection of primitives, e.g. in a {@link DataSet}
 *
 * This class does not do any synchronization.
 * @author Michael Zangl
 * @since 12048
 */
public class QuadBucketPrimitiveStore {
    /**
     * All nodes goes here, even when included in other data (ways etc). This enables the instant
     * conversion of the whole DataSet by iterating over this data structure.
     */
    private final QuadBuckets<Node> nodes = new QuadBuckets<>();

    /**
     * All ways (Streets etc.) in the DataSet.
     *
     * The way nodes are stored only in the way list.
     */
    private final QuadBuckets<Way> ways = new QuadBuckets<>();

    /**
     * All relations/relationships
     */
    private final Collection<Relation> relations = new ArrayList<>();

    /**
     * Searches for nodes in the given bounding box.
     * @param bbox the bounding box
     * @return List of nodes in the given bbox. Can be empty but not null
     */
    public List<Node> searchNodes(BBox bbox) {
        return nodes.search(bbox);
    }

    /**
     * Determines if the given node can be retrieved in the data set through its bounding box. Useful for dataset consistency test.
     * For efficiency reasons this method does not lock the dataset, you have to lock it manually.
     *
     * @param n The node to search
     * @return {@code true} if {@code n} ban be retrieved in this data set, {@code false} otherwise
     * @since 7501
     */
    public boolean containsNode(Node n) {
        return nodes.contains(n);
    }

    /**
     * Searches for ways in the given bounding box.
     * @param bbox the bounding box
     * @return List of ways in the given bbox. Can be empty but not null
     */
    public List<Way> searchWays(BBox bbox) {
        return ways.search(bbox);
    }

    /**
     * Determines if the given way can be retrieved in the data set through its bounding box. Useful for dataset consistency test.
     * For efficiency reasons this method does not lock the dataset, you have to lock it manually.
     *
     * @param w The way to search
     * @return {@code true} if {@code w} ban be retrieved in this data set, {@code false} otherwise
     * @since 7501
     */
    public boolean containsWay(Way w) {
        return ways.contains(w);
    }

    /**
     * Searches for relations in the given bounding box.
     * @param bbox the bounding box
     * @return List of relations in the given bbox. Can be empty but not null
     */
    public List<Relation> searchRelations(BBox bbox) {
        // QuadBuckets might be useful here (don't forget to do reindexing after some of rm is changed)
        return relations.stream()
                .filter(r -> r.getBBox().intersects(bbox))
                .collect(Collectors.toList());
    }

    /**
     * Determines if the given relation can be retrieved in the data set through its bounding box. Useful for dataset consistency test.
     * For efficiency reasons this method does not lock the dataset, you have to lock it manually.
     *
     * @param r The relation to search
     * @return {@code true} if {@code r} ban be retrieved in this data set, {@code false} otherwise
     * @since 7501
     */
    public boolean containsRelation(Relation r) {
        return relations.contains(r);
    }

    /**
     * Adds a primitive to this quad bucket store
     *
     * @param primitive the primitive.
     */
    public void addPrimitive(OsmPrimitive primitive) {
        boolean success = false;
        if (primitive instanceof Node) {
            success = nodes.add((Node) primitive);
        } else if (primitive instanceof Way) {
            success = ways.add((Way) primitive);
        } else if (primitive instanceof Relation) {
            success = relations.add((Relation) primitive);
        }
        if (!success) {
            throw new JosmRuntimeException("failed to add primitive: "+primitive);
        }
    }

    protected void removePrimitive(OsmPrimitive primitive) {
        boolean success = false;
        if (primitive instanceof Node) {
            success = nodes.remove(primitive);
        } else if (primitive instanceof Way) {
            success = ways.remove(primitive);
        } else if (primitive instanceof Relation) {
            success = relations.remove(primitive);
        }
        if (!success) {
            throw new JosmRuntimeException("failed to remove primitive: "+primitive);
        }
    }

    /**
     * Re-index the relation after it's position was changed.
     * @param node The node to re-index
     * @param newCoor The new coordinates
     * @param eastNorth The new east/north position
     */
    protected void reindexNode(Node node, LatLon newCoor, EastNorth eastNorth) {
        if (!nodes.remove(node))
            throw new JosmRuntimeException("Reindexing node failed to remove");
        node.setCoorInternal(newCoor, eastNorth);
        if (!nodes.add(node))
            throw new JosmRuntimeException("Reindexing node failed to add");
        for (OsmPrimitive primitive: node.getReferrers()) {
            if (primitive instanceof Way) {
                reindexWay((Way) primitive);
            } else {
                reindexRelation((Relation) primitive);
            }
        }
    }

    /**
     * Re-index the way after it's position was changed.
     * @param way The way to re-index
     */
    protected void reindexWay(Way way) {
        BBox before = way.getBBox();
        if (!ways.remove(way))
            throw new JosmRuntimeException("Reindexing way failed to remove");
        way.updatePosition();
        if (!ways.add(way))
            throw new JosmRuntimeException("Reindexing way failed to add");
        if (!way.getBBox().equals(before)) {
            for (OsmPrimitive primitive: way.getReferrers()) {
                reindexRelation((Relation) primitive);
            }
        }
    }

    /**
     * Re-index the relation after it's position was changed.
     * @param relation The relation to re-index
     */
    protected static void reindexRelation(Relation relation) {
        BBox before = relation.getBBox();
        relation.updatePosition();
        if (!before.equals(relation.getBBox())) {
            for (OsmPrimitive primitive: relation.getReferrers()) {
                reindexRelation((Relation) primitive);
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
