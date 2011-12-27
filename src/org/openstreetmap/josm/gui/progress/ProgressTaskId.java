// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.progress;

public class ProgressTaskId {

    private final String id;

    public ProgressTaskId(String component, String task) {
        this.id = component + "." + task;
    }

    public String getId() {
        return id;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ProgressTaskId other = (ProgressTaskId) obj;
        return other.id.equals(id);

    }

}
