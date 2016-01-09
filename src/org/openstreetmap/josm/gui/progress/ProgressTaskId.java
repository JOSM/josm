// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.progress;

import java.util.Objects;

public class ProgressTaskId {

    private final String id;

    public ProgressTaskId(String component, String task) {
        this.id = component + '.' + task;
    }

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
