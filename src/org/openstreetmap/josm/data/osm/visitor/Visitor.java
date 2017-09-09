// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor;

import org.openstreetmap.josm.data.osm.Changeset;

/**
 * Implementation of the visitor scheme. Every @{link org.openstreetmap.josm.data.OsmPrimitive}
 * can be visited by several different visitors.
 * @since 8
 * @deprecated class will be removed (use {@link OsmPrimitiveVisitor} if suitable)
 */
@Deprecated
public interface Visitor extends OsmPrimitiveVisitor {
    /**
     * Visiting call for changesets.
     * @param cs The changeset to inspect.
     * @since 1523
     */
    void visit(Changeset cs);
}
