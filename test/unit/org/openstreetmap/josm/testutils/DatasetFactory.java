// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;

/**
 * Convenience class allowing to manage primitives in the dataset. Useful especially for tests
 */
public class DatasetFactory {

    private final DataSet ds;

    /**
     * Constructs a new {@code DatasetFactory} with a new dataset.
     */
    public DatasetFactory() {
        this(new DataSet());
    }

    /**
     * Constructs a new {@code DatasetFactory} with a given dataset.
     * @param ds existing dataset to wrap
     */
    public DatasetFactory(DataSet ds) {
        this.ds = ds;
    }

    /**
     * Replies node with given id.
     * @param id node id
     * @return node with given id
     */
    public Node getNode(long id) {
        return (Node) ds.getPrimitiveById(id, OsmPrimitiveType.NODE);
    }

    /**
     * Replies way with given id.
     * @param id way id
     * @return way with given id
     */
    public Way getWay(long id) {
        return (Way) ds.getPrimitiveById(id, OsmPrimitiveType.WAY);
    }

    /**
     * Replies relation with given id.
     * @param id relation id
     * @return relation with given id
     */
    public Relation getRelation(long id) {
        return (Relation) ds.getPrimitiveById(id, OsmPrimitiveType.RELATION);
    }

    /**
     * Adds node with given id.
     * @param id node id
     * @return created node
     */
    public Node addNode(long id) {
        return addNode(id, 0);
    }

    /**
     * Adds way with given id.
     * @param id way id
     * @return created way
     */
    public Way addWay(long id) {
        return addWay(id, 0);
    }

    /**
     * Adds relation with given id.
     * @param id relation id
     * @return created relation
     */
    public Relation addRelation(long id) {
        return addRelation(id, 0);
    }

    /**
     * Adds node with given id and version.
     * @param id node id
     * @param version node version
     * @return created node
     */
    public Node addNode(long id, int version) {
        Node n = new Node(id, version);
        ds.addPrimitive(n);
        return n;
    }

    /**
     * Adds way with given id and version.
     * @param id way id
     * @param version way version
     * @return created way
     */
    public Way addWay(long id, int version) {
        Way w = new Way(id, version);
        ds.addPrimitive(w);
        return w;
    }

    /**
     * Adds relation with given id and version.
     * @param id relation id
     * @param version relation version
     * @return created relation
     */
    public Relation addRelation(long id, int version) {
        Relation e = new Relation(id, version);
        ds.addPrimitive(e);
        return e;
    }
}
