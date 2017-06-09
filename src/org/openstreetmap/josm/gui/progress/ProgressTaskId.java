// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.progress;

import java.util.Objects;

/**
 * The ID of a progress task. It is required to run tasks in background
 */
public class ProgressTaskId {

    private final String id;

    /**
     * Create a new {@link ProgressTaskId}
     * @param component The JOSM component name that creates this id
     * @param task The task name
     */
    public ProgressTaskId(String component, String task) {
        this.id = component + '.' + task;
    }

    /**
     * Gets the id
     * @return The task id
     */
    public String getId() {
        return id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ProgressTaskId that = (ProgressTaskId) obj;
        return Objects.equals(id, that.id);
    }
}
