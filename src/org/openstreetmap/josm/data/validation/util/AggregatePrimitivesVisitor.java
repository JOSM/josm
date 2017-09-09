// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.util;

import java.util.Collection;
import java.util.HashSet;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.OsmPrimitiveVisitor;

/**
 * A visitor that aggregates all primitives it visits.
 * <p>
 * The primitives are sorted according to their type: first nodes, then ways.
 *
 * @author frsantos
 */
public class AggregatePrimitivesVisitor implements OsmPrimitiveVisitor {
    /** Aggregated data */
    private final Collection<OsmPrimitive> aggregatedData = new HashSet<>();

    /**
     * Visits a collection of primitives
     * @param data The collection of primitives
     * @return The aggregated primitives
     */
    public Collection<OsmPrimitive> visit(Collection<OsmPrimitive> data) {
        for (OsmPrimitive osm : data) {
            osm.accept(this);
        }

        return aggregatedData;
    }

    @Override
    public void visit(Node n) {
        if (!aggregatedData.contains(n)) {
            aggregatedData.add(n);
        }
    }

    @Override
    public void visit(Way w) {
        if (!aggregatedData.contains(w)) {
            aggregatedData.add(w);
            for (Node n : w.getNodes()) {
                visit(n);
            }
        }
    }

    @Override
    public void visit(Relation r) {
        if (!aggregatedData.contains(r)) {
            aggregatedData.add(r);
            for (RelationMember m : r.getMembers()) {
                m.getMember().accept(this);
            }
        }
    }
}
