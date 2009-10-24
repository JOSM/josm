// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * This class can be used to save properties of OsmPrimitive. The main difference between PrimitiveData
 * and OsmPrimitive is that PrimitiveData is not part of the dataset and changes in PrimitiveData are not
 * reported by events
 *
 */
public abstract class PrimitiveData implements Tagged {

    // Useful?
    //private boolean disabled;
    //private boolean filtered;
    //private boolean selected;
    //private boolean highlighted;

    public PrimitiveData() {
        id = OsmPrimitive.generateUniqueId();
    }

    public PrimitiveData(PrimitiveData data) {
        this.keys.putAll(data.keys);
        this.modified = data.modified;
        this.visible = data.visible;
        this.deleted = data.deleted;
        this.id = data.id;
        this.user = data.user;
        this.version = data.version;
        this.timestamp = data.timestamp;
    }

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

    public void clearOsmId() {
        id = OsmPrimitive.generateUniqueId();
    }

    public abstract PrimitiveData makeCopy();

    public abstract OsmPrimitive makePrimitive(DataSet dataSet);

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

    // Tagged implementation

    public String get(String key) {
        return keys.get(key);
    }

    public boolean hasKeys() {
        return !keys.isEmpty();
    }

    public Collection<String> keySet() {
        return keys.keySet();
    }

    public void put(String key, String value) {
        keys.put(key, value);
    }

    public void remove(String key) {
        keys.remove(key);
    }

    public void removeAll() {
        keys.clear();
    }

    public void setKeys(Map<String, String> keys) {
        this.keys.clear();
        this.keys.putAll(keys);
    }


    @SuppressWarnings("unchecked")
    static public <T extends PrimitiveData>  List<T> getFilteredList(Collection<T> list, OsmPrimitiveType type) {
        List<T> ret = new ArrayList<T>();
        for(PrimitiveData p: list) {
            if (type.getDataClass().isInstance(p)) {
                ret.add((T)p);
            }
        }
        return ret;
    }

    protected void setKeysAsList(String... keys) {
        assert keys.length % 2 == 0;
        for (int i=0; i<keys.length/2; i++) {
            this.keys.put(keys[i * 2], keys[i * 2 + 1]);
        }
    }



}
