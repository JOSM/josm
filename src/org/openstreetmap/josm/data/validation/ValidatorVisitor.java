// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.data.validation;

import java.util.List;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.WaySegment;

public interface ValidatorVisitor {
    void visit(TestError error);
    void visit(OsmPrimitive p);
    void visit(WaySegment ws);
    void visit(List<Node> nodes);
}
