// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.upload;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.openstreetmap.josm.data.osm.Relation;

public class CyclicUploadDependencyException extends Exception {
    private Stack<Relation> cycle;

    public CyclicUploadDependencyException(Stack<Relation> cycle) {
        super();
        this.cycle = cycle;
    }

    protected String formatRelation(Relation r) {
        StringBuilder sb = new StringBuilder();
        if (r.getName() != null) {
            sb.append("'").append(r.getName()).append("'");
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
        sb.append(tr("Cyclic dependency between relations:"));
        sb.append("[");
        for (int i=0; i< cycle.size(); i++) {
            if (i >0 ) {
                sb.append(",");
            }
            sb.append(formatRelation(cycle.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    public List<Relation> getCyclicUploadDependency() {
        return new ArrayList<Relation>(cycle);
    }
}
