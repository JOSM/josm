// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.upload;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import org.openstreetmap.josm.data.osm.Relation;

/**
 * This is an exception that is thrown if the user attempts to upload a list of relations with a cyclic dependency in them
 */
public class CyclicUploadDependencyException extends Exception {
    private final List<Relation> cycle;

    /**
     * Creates a new {@link CyclicUploadDependencyException}
     * @param cycle The cycle that was found
     */
    public CyclicUploadDependencyException(Stack<Relation> cycle) {
        this.cycle = new ArrayList<>(cycle);
    }

    protected String formatRelation(Relation r) {
        StringBuilder sb = new StringBuilder();
        if (r.getName() != null) {
            sb.append('\'').append(r.getName()).append('\'');
        } else if (!r.isNew()) {
            sb.append(r.getId());
        } else {
            sb.append("relation@").append(r.hashCode());
        }
        return sb.toString();
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append(tr("Cyclic dependency between relations:"))
          .append('[');
        for (int i = 0; i < cycle.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(formatRelation(cycle.get(i)));
        }
        sb.append(']');
        return sb.toString();
    }

    /**
     * Gets the cycle
     * @return The cycle that was detected
     */
    public List<Relation> getCyclicUploadDependency() {
        return Collections.unmodifiableList(cycle);
    }
}
