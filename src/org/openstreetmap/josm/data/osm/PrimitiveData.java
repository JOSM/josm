// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
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
public abstract class PrimitiveData implements IPrimitive {

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
        this.incomplete = data.incomplete;
    }

    private final Map<String, String> keys = new HashMap<String, String>();
    private boolean modified;
    private boolean visible = true;
    private boolean deleted;
    private boolean incomplete;
    private long id;
    private User user;
    private int version;
    private Date timestamp = new Date();
    private int changesetId;

    @Override
    public boolean isModified() {
        return modified;
    }
    @Override
    public void setModified(boolean modified) {
        this.modified = modified;
    }
    @Override
    public boolean isVisible() {
        return visible;
    }
    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    @Override
    public boolean isDeleted() {
        return deleted;
    }
    @Override
    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
    @Override
    public long getId() {
        return id > 0 ? id : 0;
    }
    public void setId(long id) {
        this.id = id;
    }
    @Override
    public User getUser() {
        return user;
    }
    @Override
    public void setUser(User user) {
        this.user = user;
    }
    @Override
    public int getVersion() {
        return version;
    }
    public void setVersion(int version) {
        this.version = version;
    }
    @Override
    public void setOsmId(long id, int version) {
        if (id <= 0)
            throw new IllegalArgumentException(tr("ID > 0 expected. Got {0}.", id));
        if (version <= 0)
            throw new IllegalArgumentException(tr("Version > 0 expected. Got {0}.", version));
        this.id = id;
        this.version = version;
        this.setIncomplete(false);
    }
    @Override
    public Date getTimestamp() {
        return timestamp;
    }
    @Override
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
    @Override
    public boolean isTimestampEmpty() {
        return timestamp == null || timestamp.getTime() == 0;
    }

    @Override
    public int getChangesetId() {
        return changesetId;
    }

    @Override
    public void setChangesetId(int changesetId) {
        this.changesetId = changesetId;
    }

    @Override
    public Map<String, String> getKeys() {
        return keys;
    }
    @Override
    public boolean isIncomplete() {
        return incomplete;
    }
    public void setIncomplete(boolean incomplete) {
        this.incomplete = incomplete;
    }

    public void clearOsmId() {
        id = OsmPrimitive.generateUniqueId();
    }

    public abstract PrimitiveData makeCopy();

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
        if (incomplete) {
            builder.append("I");
        }

        return builder.toString();
    }

    // Tagged implementation

    @Override
    public String get(String key) {
        return keys.get(key);
    }

    @Override
    public boolean hasKeys() {
        return !keys.isEmpty();
    }

    @Override
    public Collection<String> keySet() {
        return keys.keySet();
    }

    @Override
    public void put(String key, String value) {
        keys.put(key, value);
    }

    @Override
    public void remove(String key) {
        keys.remove(key);
    }

    @Override
    public void removeAll() {
        keys.clear();
    }

    @Override
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

    /**
     * PrimitiveId implementation. Returns the same value as getId()
     */
    @Override
    public long getUniqueId() {
        return id;
    }

    /**
     * Returns a PrimitiveId object for this primitive
     *
     * @return the PrimitiveId for this primitive
     */
    public PrimitiveId getPrimitiveId() {
        return new SimplePrimitiveId(getUniqueId(), getType());
    }

    @Override
    public boolean isNew() {
        return id <= 0;
    }

    @Override
    public abstract OsmPrimitiveType getType();
}
