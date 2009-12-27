// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;


public interface NameFormatter {
    String format(Node node);
    String format(Way way);
    String format(Relation relation);
    String format(Changeset changeset);
}
