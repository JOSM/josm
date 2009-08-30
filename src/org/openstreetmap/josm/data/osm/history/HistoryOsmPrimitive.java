// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.openstreetmap.josm.data.osm.OsmPrimitiveType;

/**
 * Represents an immutable OSM primitive in the context of a historical view on
 * OSM data.
 *
 */
public abstract class HistoryOsmPrimitive implements Comparable<HistoryOsmPrimitive> {

    private long id;
    private boolean visible;
    private String user;
    private long uid;
    private long changesetId;
    private Date timestamp;
    private long version;
    private HashMap<String, String> tags;

    protected void ensurePositiveLong(long value, String name) {
        if (value <= 0)
            throw new IllegalArgumentException(tr("parameter ''{0}'' > 0 expected, got ''{1}''", name, value));
    }

    protected void ensureNotNull(Object obj, String name) {
        if (obj == null)
            throw new IllegalArgumentException(tr("parameter ''{0}'' must not be null", name));
    }

    /**
     * constructor
     * 
     * @param id the id (>0 required)
     * @param version the version (> 0 required)
     * @param visible whether the primitive is still visible
     * @param user  the user (! null required)
     * @param uid the user id (> 0 required)
     * @param changesetId the changeset id (> 0 required)
     * @param timestamp the timestamp (! null required)
     * 
     * @throws IllegalArgumentException thrown if preconditions are violated
     */
    public HistoryOsmPrimitive(long id, long version, boolean visible, String user, long uid, long changesetId, Date timestamp) throws IllegalArgumentException {
        ensurePositiveLong(id, "id");
        ensurePositiveLong(version, "version");
        ensurePositiveLong(uid, "uid");
        ensurePositiveLong(changesetId, "changesetId");
        ensureNotNull(user, "user");
        ensureNotNull(timestamp, "timestamp");
        this.id = id;
        this.version = version;
        this.visible = visible;
        this.user = user;
        this.uid = uid;
        this.changesetId  = changesetId;
        this.timestamp = timestamp;
        tags = new HashMap<String, String>();
    }

    public long getId() {
        return id;
    }
    public boolean isVisible() {
        return visible;
    }
    public String getUser() {
        return user;
    }
    public long getUid() {
        return uid;
    }
    public long getChangesetId() {
        return changesetId;
    }
    public Date getTimestamp() {
        return timestamp;
    }

    public long getVersion() {
        return version;
    }

    public boolean matches(long id, long version) {
        return this.id == id && this.version == version;
    }

    public boolean matches(long id) {
        return this.id == id;
    }

    public abstract OsmPrimitiveType getType();

    public int compareTo(HistoryOsmPrimitive o) {
        if (this.id != o.id)
            throw new ClassCastException(tr("can't compare primitive with id ''{0}'' to primitive with id ''{1}''", o.id, this.id));
        return new Long(this.version).compareTo(o.version);
    }

    public void put(String key, String value) {
        tags.put(key, value);
    }

    public String get(String key) {
        return tags.get(key);
    }

    public boolean hasTag(String key) {
        return tags.get(key) != null;
    }

    public Map<String,String> getTags() {
        return Collections.unmodifiableMap(tags);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (id ^ (id >>> 32));
        result = prime * result + (int) (version ^ (version >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        // equal semantics is valid for subclasses like {@see HistoryOsmNode} etc. too.
        // So, don't enforce equality of class.
        //
        //        if (getClass() != obj.getClass())
        //            return false;
        HistoryOsmPrimitive other = (HistoryOsmPrimitive) obj;
        if (id != other.id)
            return false;
        if (version != other.version)
            return false;
        return true;
    }
}
