// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;

/**
 * MergeSourceBuildingVisitor helps to build the "hull" of a collection of {@see OsmPrimitive}s
 * which shall be merged into another layer. The "hull" is slightly bigger than the original
 * collection. It includes, for instance the nodes of a way in the original collection even though
 * these nodes might not be present explicitly in the original collection. The "hull" also includes
 * incomplete {@see OsmPrimitive}s which are referred to by relations in the original collection. And
 * it turns {@see OsmPrimitive} referred to by {@see Relation}s in the original collection into
 * incomplete {@see OsmPrimitive}s in the "hull", if they are not themselves present in the
 * original collection.
 *
 */
public class MergeSourceBuildingVisitor extends AbstractVisitor {
    private DataSet selectionBase;
    private DataSet hull;
    private HashMap<OsmPrimitive, OsmPrimitive> mappedPrimitives;

    /**
     * Creates the visitor. The visitor starts to build the "hull" from
     * the currently selected primitives in the dataset <code>selectionBase</code>,
     * i.e. from {@see DataSet#getSelected()}.
     *
     * @param selectionBase the dataset. Must not be null.
     * @exception IllegalArgumentException thrown if selectionBase is null
     *
     */
    public MergeSourceBuildingVisitor(DataSet selectionBase) throws IllegalArgumentException {
        if (selectionBase == null)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' must not be null.", "selectionBase"));
        this.selectionBase = selectionBase;
        this.hull = new DataSet();
        this.mappedPrimitives = new HashMap<OsmPrimitive, OsmPrimitive>();
    }

    /**
     * Remebers a node in the "hull"
     *
     * @param n the node
     */
    protected void rememberNode(Node n) {
        if (isAlreadyRemembered(n))
            return;
        Node clone = new Node(n);
        mappedPrimitives.put(n, clone);
    }

    /**
     * remembers a way in the hull
     *
     * @param w the way
     */
    protected void rememberWay(Way w) {
        if (isAlreadyRemembered(w))
            return;
        Way clone = new Way(w);
        List<Node> newNodes = new ArrayList<Node>();
        for (Node n: w.getNodes()) {
            newNodes.add((Node)mappedPrimitives.get(n));
        }
        clone.setNodes(newNodes);
        mappedPrimitives.put(w, clone);
    }

    /**
     * Remembers a relation in the hull
     *
     * @param r the relation
     */
    protected void rememberRelation(Relation r) {
        Relation clone;
        if (mappedPrimitives.keySet().contains(r)) {
            clone = (Relation)mappedPrimitives.get(r);
            clone.cloneFrom(r);
        } else {
            clone = new Relation(r);
        }

        List<RelationMember> newMembers = new ArrayList<RelationMember>();
        for (RelationMember member: r.getMembers()) {
            newMembers.add(
                    new RelationMember(member.getRole(), mappedPrimitives.get(member.getMember())));

        }
        clone.setMembers(newMembers);

        if (! mappedPrimitives.keySet().contains(r)) {
            mappedPrimitives.put(r, clone);
        }
    }

    protected void rememberRelationPartial(Relation r) {
        if (isAlreadyRemembered(r))
            return;
        Relation clone = new Relation(r);
        clone.setMembers(null);
        mappedPrimitives.put(r, clone);
    }

    protected void rememberIncomplete(OsmPrimitive primitive) {
        if (isAlreadyRemembered(primitive))
            return;
        OsmPrimitive clone = null;
        if (primitive instanceof Node) {
            clone = new Node(primitive.getId());
        } else if (primitive instanceof Way) {
            clone = new Way(primitive.getId());
        } else if (primitive instanceof Relation) {
            clone = new Relation(primitive.getId());
        }
        mappedPrimitives.put(primitive, clone);
    }

    protected void rememberNodeIncomplete(Node n) {
        if (isAlreadyRemembered(n))
            return;
        Node clone = new Node(n);
        clone.incomplete = true;
        mappedPrimitives.put(n, clone);
    }

    protected void rememberWayIncomplete(Way w) {
        if (isAlreadyRemembered(w))
            return;
        Way clone = new Way(w);
        clone.setNodes(null);
        clone.incomplete = true;
        mappedPrimitives.put(w, clone);
    }

    protected void rememberRelationIncomplete(Relation r) {
        Relation clone;
        if (isAlreadyRemembered(r)) {
            clone = (Relation)mappedPrimitives.get(r);
            clone.cloneFrom(r);
        } else {
            clone = new Relation(r);
        }
        clone.setMembers(null);
        clone.incomplete = true;
        if (! isAlreadyRemembered(r)) {
            mappedPrimitives.put(r, clone);
        }
    }

    public void visit(Node n) {
        rememberNode(n);
    }

    public void visit(Way w) {
        // remember all nodes this way refers to ...
        //
        for (Node n: w.getNodes()) {
            if (! isAlreadyRemembered(n)) {
                n.visit(this);
            }
        }
        // ... and the way itself
        rememberWay(w);
    }

    protected boolean isInSelectionBase(OsmPrimitive primitive) {
        return selectionBase.getSelected().contains(primitive);
    }

    protected boolean isAlreadyRemembered(OsmPrimitive primitive) {
        return mappedPrimitives.keySet().contains(primitive);
    }

    public void visit(Relation r) {
        // first, remember all primitives members refer to (only if necessary, see
        // below)
        //
        rememberRelationPartial(r);
        for (RelationMember member: r.getMembers()) {
            if (isAlreadyRemembered(member.getMember())) {
                // referred primitive already remembered
                //
                continue;
            }
            if (member.isNode()) {
                Node node = member.getNode();
                if (isInSelectionBase(node)) {
                    rememberNode(node);
                } else if (node.isNew()) {
                    rememberNode(node);
                } else  {
                    rememberNodeIncomplete(node);
                }
            } else if (member.isWay()) {
                Way way = member.getWay();
                if (isInSelectionBase(way)) {
                    way.visit(this);
                } else if (way.isNew()) {
                    way.visit(this);
                } else {
                    rememberWayIncomplete(way);
                }
            } else if (member.isRelation()) {
                Relation relation = member.getRelation();
                if (isInSelectionBase(member.getMember())) {
                    relation.visit(this);
                } else if (relation.isNew()) {
                    relation.visit(this);
                } else {
                    rememberRelationIncomplete(relation);
                }
            }
        }
        rememberRelation(r);
    }

    protected void buildHull() {
        for (OsmPrimitive primitive : mappedPrimitives.keySet()) {
            OsmPrimitive clone = mappedPrimitives.get(primitive);
            hull.addPrimitive(clone);
        }
    }

    public DataSet build() {
        for (OsmPrimitive primitive: selectionBase.getSelected()) {
            primitive.visit(this);
        }
        buildHull();
        return hull;
    }
}
