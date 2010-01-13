// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.tools.CheckParameterUtil;

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
            throw new IllegalArgumentException(MessageFormat.format("Parameter ''{0}'' > 0 expected. Got ''{1}''.", name, value));
    }

    /**
     * constructor
     *
     * @param id the id (>0 required)
     * @param version the version (> 0 required)
     * @param visible whether the primitive is still visible
     * @param user  the user (! null required)
     * @param uid the user id (> 0 required)
     * @param changesetId the changeset id (may be null if the changeset isn't known)
     * @param timestamp the timestamp (! null required)
     *
     * @throws IllegalArgumentException thrown if preconditions are violated
     */
    public HistoryOsmPrimitive(long id, long version, boolean visible, String user, long uid, long changesetId, Date timestamp) throws IllegalArgumentException {
        ensurePositiveLong(id, "id");
        ensurePositiveLong(version, "version");
        if(uid != -1) {
            ensurePositiveLong(uid, "uid");
        }
        CheckParameterUtil.ensureParameterNotNull(user, "user");
        CheckParameterUtil.ensureParameterNotNull(timestamp, "timestamp");
        this.id = id;
        this.version = version;
        this.visible = visible;
        this.user = user;
        this.uid = uid;
        // FIXME: restrict to IDs > 0 as soon as OsmPrimitive holds the
        // changeset id too
        this.changesetId  = changesetId;
        this.timestamp = timestamp;
        tags = new HashMap<String, String>();
    }

    public long getId() {
        return id;
    }

    public PrimitiveId getPrimitiveId() {
        return new SimplePrimitiveId(id, getType());
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
            throw new ClassCastException(tr("Cannot compare primitive with ID ''{0}'' to primitive with ID ''{1}''.", o.id, this.id));
        return Long.valueOf(this.version).compareTo(o.version);
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

    /**
     * Sets the tags for this history primitive. Removes all
     * tags if <code>tags</code> is null.
     *
     * @param tags the tags. May be null.
     */
    public void setTags(Map<String,String> tags) {
        if (tags == null) {
            this.tags = new HashMap<String, String>();
        } else {
            this.tags = new HashMap<String, String>(tags);
        }
    }

    /**
     * Replies the name of this primitive. The default implementation replies the value
     * of the tag <tt>name</tt> or null, if this tag is not present.
     *
     * @return the name of this primitive
     */
    public String getName() {
        if (get("name") != null)
            return get("name");
        return null;
    }

    /**
     * Replies the display name of a primitive formatted by <code>formatter</code>
     *
     * @return the display name
     */
    public abstract String getDisplayName(HistoryNameFormatter formatter);

    /**
     * Replies the a localized name for this primitive given by the value of the tags (in this order)
     * <ul>
     *   <li>name:lang_COUNTRY_Variant  of the current locale</li>
     *   <li>name:lang_COUNTRY of the current locale</li>
     *   <li>name:lang of the current locale</li>
     *   <li>name of the current locale</li>
     * </ul>
     *
     * null, if no such tag exists
     *
     * @return the name of this primitive
     */
    public String getLocalName() {
        String key = "name:" + Locale.getDefault().toString();
        if (get(key) != null)
            return get(key);
        key = "name:" + Locale.getDefault().getLanguage() + "_" + Locale.getDefault().getCountry();
        if (get(key) != null)
            return get(key);
        key = "name:" + Locale.getDefault().getLanguage();
        if (get(key) != null)
            return get(key);
        return getName();
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
        if (!(obj instanceof HistoryOsmPrimitive))
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
