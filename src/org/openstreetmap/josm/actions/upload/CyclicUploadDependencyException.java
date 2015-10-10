// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.upload;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayDeque;
import java.util.Deque;

import org.openstreetmap.josm.data.osm.Relation;

public class CyclicUploadDependencyException extends Exception {
    private final Deque<Relation> cycle;

    public CyclicUploadDependencyException(Deque<Relation> cycle) {
        this.cycle = cycle;
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
        for (Relation r : cycle) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(formatRelation(r));
        }
        sb.append(']');
        return sb.toString();
    }

    public Deque<Relation> getCyclicUploadDependency() {
        return new ArrayDeque<>(cycle);
    }
}
