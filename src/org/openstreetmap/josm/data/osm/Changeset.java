// License: GPL. Copyright 2007 by Martijn van Oosterhout and others
package org.openstreetmap.josm.data.osm;

import org.openstreetmap.josm.data.osm.visitor.Visitor;

/**
 * Represents a single changeset in JOSM. For now its only used during
 * upload but in the future we may do more.
 *
 */
public final class Changeset extends OsmPrimitive {
    /**
     * Time of last modification to this object. This is not set by JOSM but
     * read from the server and delivered back to the server unmodified.
     */
    public String end_timestamp = null;

    /**
     * Time of first modification to this object. This is not set by JOSM but
     * read from the server and delivered back to the server unmodified.
     */
    public String start_timestamp = null;

    public void visit(Visitor v) {
        v.visit(this);
    }

    public int compareTo(OsmPrimitive arg0) {
        if (arg0 instanceof Changeset) return Long.valueOf(id).compareTo(arg0.id);
        return 1;
    }
}
