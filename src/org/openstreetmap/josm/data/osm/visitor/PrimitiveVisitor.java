package org.openstreetmap.josm.data.osm.visitor;

import org.openstreetmap.josm.data.osm.INode;
import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.data.osm.IWay;

public interface PrimitiveVisitor {
    void visit(INode n);
    void visit(IWay w);
    void visit(IRelation e);
}
