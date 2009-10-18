// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * This class can be used to save properties of OsmPrimitive. The main difference between PrimitiveData
 * and OsmPrimitive is that PrimitiveData is not part of the dataset and changes in PrimitiveData are not
 * reported by events
 *
 */
public abstract class PrimitiveData {

    // Useful?
    //private boolean disabled;
    //private boolean filtered;
    //private boolean selected;
    //private boolean highlighted;

    private final Map<String, String> keys = new HashMap<String, String>();
    private boolean modified;
    private boolean visible;
    private boolean deleted;
    private long id;
    private User user;
    private int version;
    private int timestamp;

    public boolean isModified() {
        return modified;
    }
    public void setModified(boolean modified) {
        this.modified = modified;
    }
    public boolean isVisible() {
        return visible;
    }
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    public boolean isDeleted() {
        return deleted;
    }
    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public User getUser() {
        return user;
    }
    public void setUser(User user) {
        this.user = user;
    }
    public int getVersion() {
        return version;
    }
    public void setVersion(int version) {
        this.version = version;
    }
    public int getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }
    public Map<String, String> getKeys() {
        return keys;
    }


    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append(id).append(keys);
        if (modified) {
            builder.append("M");
        }
        if (visible) {
            builder.append("V");
        }
        if (deleted) {
            builder.append("D");
        }

        return builder.toString();
    }


}
