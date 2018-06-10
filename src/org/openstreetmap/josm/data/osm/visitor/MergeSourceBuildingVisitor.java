// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.NodeData;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveData;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationData;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.RelationMemberData;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WayData;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * MergeSourceBuildingVisitor helps to build the "hull" of a collection of {@link OsmPrimitive}s
 * which shall be merged into another layer. The "hull" is slightly bigger than the original
 * collection. It includes, for instance the nodes of a way in the original collection even though
 * these nodes might not be present explicitly in the original collection. The "hull" also includes
 * incomplete {@link OsmPrimitive}s which are referred to by relations in the original collection. And
 * it turns {@link OsmPrimitive} referred to by {@link Relation}s in the original collection into
 * incomplete {@link OsmPrimitive}s in the "hull", if they are not themselves present in the original collection.
 * @since 1891
 */
public class MergeSourceBuildingVisitor implements OsmPrimitiveVisitor {
    private final DataSet selectionBase;
    private final DataSet hull;
    private final Map<OsmPrimitive, PrimitiveData> mappedPrimitives;

    /**
     * Creates the visitor. The visitor starts to build the "hull" from
     * the currently selected primitives in the dataset <code>selectionBase</code>,
     * i.e. from {@link DataSet#getSelected()}.
     *
     * @param selectionBase the dataset. Must not be null.
     * @throws IllegalArgumentException if selectionBase is null
     */
    public MergeSourceBuildingVisitor(DataSet selectionBase) {
        CheckParameterUtil.ensureParameterNotNull(selectionBase, "selectionBase");
        this.selectionBase = selectionBase;
        this.hull = new DataSet();
        this.mappedPrimitives = new HashMap<>();
    }

    protected boolean isInSelectionBase(OsmPrimitive primitive) {
        return selectionBase.getAllSelected().contains(primitive);
    }

    protected boolean isAlreadyRemembered(OsmPrimitive primitive) {
        return mappedPrimitives.containsKey(primitive);
    }

    /**
     * Remebers a node in the "hull"
     *
     * @param n the node
     */
    protected void rememberNode(Node n) {
        if (isAlreadyRemembered(n))
            return;
        mappedPrimitives.put(n, n.save());
    }

    /**
     * remembers a way in the hull
     *
     * @param w the way
     */
    protected void rememberWay(Way w) {
        if (isAlreadyRemembered(w))
            return;
        WayData clone = w.save();
        List<Long> newNodes = new ArrayList<>(w.getNodesCount());
        for (Node n: w.getNodes()) {
            newNodes.add(mappedPrimitives.get(n).getUniqueId());
        }
        clone.setNodeIds(newNodes);
        mappedPrimitives.put(w, clone);
    }

    /**
     * Remembers a relation in the hull
     *
     * @param r the relation
     */
    protected void rememberRelation(Relation r) {
        RelationData clone;
        if (isAlreadyRemembered(r)) {
            clone = (RelationData) mappedPrimitives.get(r);
        } else {
            clone = r.save();
            mappedPrimitives.put(r, clone);
        }

        List<RelationMemberData> newMembers = new ArrayList<>();
        for (RelationMember member: r.getMembers()) {
            newMembers.add(new RelationMemberData(member.getRole(), mappedPrimitives.get(member.getMember())));

        }
        clone.setMembers(newMembers);
    }

    protected void rememberRelationPartial(Relation r) {
        if (isAlreadyRemembered(r))
            return;
        RelationData clone = r.save();
        clone.getMembers().clear();
        mappedPrimitives.put(r, clone);
    }

    protected void rememberIncomplete(OsmPrimitive primitive) {
        if (isAlreadyRemembered(primitive))
            return;
        PrimitiveData clone = primitive.save();
        clone.setIncomplete(true);
        mappedPrimitives.put(primitive, clone);
    }

    @Override
    public void visit(Node n) {
        rememberNode(n);
    }

    @Override
    public void visit(Way w) {
        // remember all nodes this way refers to ...
        for (Node n: w.getNodes()) {
            n.accept(this);
        }
        // ... and the way itself
        rememberWay(w);
    }

    @Override
    public void visit(Relation r) {
        // first, remember all primitives members refer to (only if necessary, see below)
        rememberRelationPartial(r);
        for (RelationMember member: r.getMembers()) {
            if (isAlreadyRemembered(member.getMember())) {
                // referred primitive already remembered
                continue;
            }
            if (isInSelectionBase(member.getMember()) || member.getMember().isNew()) {
                member.getMember().accept(this);
            } else {
                rememberIncomplete(member.getMember());
            }
        }
        rememberRelation(r);
    }

    protected void buildHull() {
        // Create all primitives first
        for (PrimitiveData primitive: mappedPrimitives.values()) {
            OsmPrimitive newPrimitive = hull.getPrimitiveById(primitive);
            boolean created = newPrimitive == null;
            if (created) {
                newPrimitive = primitive.getType().newInstance(primitive.getUniqueId(), true);
            }
            if (newPrimitive instanceof Node && !primitive.isIncomplete()) {
                newPrimitive.load(primitive);
            }
            if (created) {
                hull.addPrimitive(newPrimitive);
            }
        }
        // Then ways and relations
        for (PrimitiveData primitive : mappedPrimitives.values()) {
            if (!(primitive instanceof NodeData) && !primitive.isIncomplete()) {
                hull.getPrimitiveById(primitive).load(primitive);
            }
        }
    }

    /**
     * Builds and returns the "hull".
     * @return the "hull" data set
     */
    public DataSet build() {
        for (OsmPrimitive primitive: selectionBase.getAllSelected()) {
            primitive.accept(this);
        }
        buildHull();
        return hull;
    }
}
