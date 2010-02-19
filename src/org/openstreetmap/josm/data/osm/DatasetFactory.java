// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

/**
 * Convenience class allowing to manage primitives in the dataset. Useful especially for tests
 *
 */
public class DatasetFactory {

    private final DataSet ds;

    public DatasetFactory() {
        ds = new DataSet();
    }

    public DatasetFactory(DataSet ds) {
        this.ds = ds;
    }

    public Node getNode(long id) {
        return (Node) ds.getPrimitiveById(id, OsmPrimitiveType.NODE);
    }

    public Way getWay(long id) {
        return (Way) ds.getPrimitiveById(id, OsmPrimitiveType.WAY);
    }

    public Relation getRelation(long id) {
        return (Relation) ds.getPrimitiveById(id, OsmPrimitiveType.RELATION);
    }

    public Node addNode(long id) {
        return addNode(id, 0);
    }

    public Way addWay(long id) {
        return addWay(id, 0);
    }

    public Relation addRelation(long id) {
        return addRelation(id, 0);
    }

    public Node addNode(long id, int version) {
        Node n = new Node(id, version);
        ds.addPrimitive(n);
        return n;
    }

    public Way addWay(long id, int version) {
        Way w = new Way(id, version);
        ds.addPrimitive(w);
        return w;
    }

    public Relation addRelation(long id, int version) {
        Relation e = new Relation(id, version);
        ds.addPrimitive(e);
        return e;
    }

}
