// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * Represents an immutable OSM primitive in the context of a historical view on
 * OSM data.
 *
 */
public abstract class HistoryOsmPrimitive implements Comparable<HistoryOsmPrimitive> {

    private long id;
    private boolean visible;
    private User user;
    private long changesetId;
    private Changeset changeset;
    private Date timestamp;
    private long version;
    private Map<String, String> tags;

    protected void ensurePositiveLong(long value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(MessageFormat.format("Parameter ''{0}'' > 0 expected. Got ''{1}''.", name, value));
        }
    }

    /**
     * Constructs a new {@code HistoryOsmPrimitive}.
     *
     * @param id the id (&gt; 0 required)
     * @param version the version (&gt; 0 required)
     * @param visible whether the primitive is still visible
     * @param user the user (!= null required)
     * @param changesetId the changeset id (&gt; 0 required)
     * @param timestamp the timestamp (!= null required)
     *
     * @throws IllegalArgumentException if preconditions are violated
     */
    public HistoryOsmPrimitive(long id, long version, boolean visible, User user, long changesetId, Date timestamp) throws IllegalArgumentException {
        this(id, version, visible, user, changesetId, timestamp, true);
    }

    /**
     * Constructs a new {@code HistoryOsmPrimitive} with a configurable checking of historic parameters.
     * This is needed to build virtual HistoryOsmPrimitives for modified primitives, which do not have a timestamp and a changeset id.
     *
     * @param id the id (&gt; 0 required)
     * @param version the version (&gt; 0 required)
     * @param visible whether the primitive is still visible
     * @param user the user (!= null required)
     * @param changesetId the changeset id (&gt; 0 required if {@code checkHistoricParams} is true)
     * @param timestamp the timestamp (!= null required if {@code checkHistoricParams} is true)
     * @param checkHistoricParams if true, checks values of {@code changesetId} and {@code timestamp}
     *
     * @throws IllegalArgumentException if preconditions are violated
     * @since 5440
     */
    public HistoryOsmPrimitive(long id, long version, boolean visible, User user, long changesetId, Date timestamp, boolean checkHistoricParams) throws IllegalArgumentException {
        ensurePositiveLong(id, "id");
        ensurePositiveLong(version, "version");
        CheckParameterUtil.ensureParameterNotNull(user, "user");
        if (checkHistoricParams) {
            ensurePositiveLong(changesetId, "changesetId");
            CheckParameterUtil.ensureParameterNotNull(timestamp, "timestamp");
        }
        this.id = id;
        this.version = version;
        this.visible = visible;
        this.user = user;
        this.changesetId  = changesetId;
        this.timestamp = timestamp;
        tags = new HashMap<String, String>();
    }

    /**
     * Constructs a new {@code HistoryOsmPrimitive} from an existing {@link OsmPrimitive}.
     * @param p the primitive
     */
    public HistoryOsmPrimitive(OsmPrimitive p) {
        this(p.getId(), p.getVersion(), p.isVisible(), p.getUser(), p.getChangesetId(), p.getTimestamp());
    }

    /**
     * Replies a new {@link HistoryNode}, {@link HistoryWay} or {@link HistoryRelation} from an existing {@link OsmPrimitive}.
     * @param p the primitive
     * @return a new {@code HistoryNode}, {@code HistoryWay} or {@code HistoryRelation} from {@code p}.
     */
    public static HistoryOsmPrimitive forOsmPrimitive(OsmPrimitive p) {
        if (p instanceof Node) {
            return new HistoryNode((Node) p);
        } else if (p instanceof Way) {
            return new HistoryWay((Way) p);
        } else if (p instanceof Relation) {
            return new HistoryRelation((Relation) p);
        } else {
            return null;
        }
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
    public User getUser() {
        return user;
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

    @Override
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

    public Changeset getChangeset() {
        return changeset;
    }

    public void setChangeset(Changeset changeset) {
        this.changeset = changeset;
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
     * @param formatter The formatter used to generate a display name
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
        // equal semantics is valid for subclasses like {@link HistoryOsmNode} etc. too.
        // So, don't enforce equality of class.
        HistoryOsmPrimitive other = (HistoryOsmPrimitive) obj;
        if (id != other.id)
            return false;
        if (version != other.version)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [version=" + version + ", id=" + id + ", visible=" + visible + ", "
                + (timestamp != null ? "timestamp=" + timestamp : "") + ", "
                + (user != null ? "user=" + user + ", " : "") + "changesetId="
                + changesetId
                + "]";
    }
}
