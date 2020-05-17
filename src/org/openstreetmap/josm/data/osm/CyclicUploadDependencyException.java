// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * This is an exception that is thrown if the user attempts to upload a list of relations with a cyclic dependency in them
 * @since 12673 (moved from {@code action.upload} package)
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
        return cycle.stream().map(this::formatRelation)
                .collect(Collectors.joining(",", tr("Cyclic dependency between relations:") + '[', "]"));
    }

    /**
     * Gets the cycle
     * @return The cycle that was detected
     */
    public List<Relation> getCyclicUploadDependency() {
        return Collections.unmodifiableList(cycle);
    }
}
