// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveData;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.data.osm.Tagged;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.date.DateUtils;

/**
 * Represents an immutable OSM primitive in the context of a historical view on OSM data.
 * @since 1670
 */
public abstract class HistoryOsmPrimitive implements Tagged, Comparable<HistoryOsmPrimitive>, PrimitiveId {

    private final long id;
    private final boolean visible;
    private final User user;
    private final long changesetId;
    private Changeset changeset;
    private final Date timestamp;
    private final long version;
    private Map<String, String> tags;

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
    protected HistoryOsmPrimitive(long id, long version, boolean visible, User user, long changesetId, Date timestamp) {
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
    protected HistoryOsmPrimitive(long id, long version, boolean visible, User user, long changesetId, Date timestamp,
            boolean checkHistoricParams) {
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
        this.changesetId = changesetId;
        this.timestamp = DateUtils.cloneDate(timestamp);
        this.tags = new HashMap<>();
    }

    /**
     * Constructs a new {@code HistoryOsmPrimitive} from an existing {@link OsmPrimitive}.
     * @param p the primitive
     */
    protected HistoryOsmPrimitive(OsmPrimitive p) {
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

    /**
     * Returns the id.
     * @return the id
     */
    public long getId() {
        return id;
    }

    /**
     * Returns the primitive id.
     * @return the primitive id
     */
    public PrimitiveId getPrimitiveId() {
        return new SimplePrimitiveId(id, getType());
    }

    /**
     * Determines if the primitive is still visible.
     * @return {@code true} if the primitive is still visible
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Returns the user.
     * @return the user
     */
    public User getUser() {
        return user;
    }

    /**
     * Returns the changeset id.
     * @return the changeset id
     */
    public long getChangesetId() {
        return changesetId;
    }

    /**
     * Returns the timestamp.
     * @return the timestamp
     */
    public Date getTimestamp() {
        return DateUtils.cloneDate(timestamp);
    }

    /**
     * Returns the version.
     * @return the version
     */
    public long getVersion() {
        return version;
    }

    /**
     * Checks that value is positive.
     * @param value value to check
     * @param name parameter name for error message
     * @throws IllegalArgumentException if {@code value <= 0}
     */
    protected final void ensurePositiveLong(long value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(MessageFormat.format("Parameter ''{0}'' > 0 expected. Got ''{1}''.", name, value));
        }
    }

    /**
     * Determines if this history matches given id and version.
     * @param id Primitive identifier
     * @param version Primitive version
     * @return {@code true} if this history matches given id and version
     */
    public boolean matches(long id, long version) {
        return this.id == id && this.version == version;
    }

    /**
     * Determines if this history matches given id.
     * @param id Primitive identifier
     * @return {@code true} if this history matches given id
     */
    public boolean matches(long id) {
        return this.id == id;
    }

    @Override
    public final long getUniqueId() {
        return getId();
    }

    @Override
    public final boolean isNew() {
        return false;
    }

    @Override
    public int compareTo(HistoryOsmPrimitive o) {
        if (this.id != o.id)
            throw new ClassCastException(tr("Cannot compare primitive with ID ''{0}'' to primitive with ID ''{1}''.", o.id, this.id));
        return Long.compare(this.version, o.version);
    }

    @Override
    public final void put(String key, String value) {
        tags.put(key, value);
    }

    @Override
    public final String get(String key) {
        return tags.get(key);
    }

    @Override
    public final boolean hasKey(String key) {
        return tags.containsKey(key);
    }

    @Override
    public final Map<String, String> getKeys() {
        return getTags();
    }

    @Override
    public final void setKeys(Map<String, String> keys) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void remove(String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void removeAll() {
        throw new UnsupportedOperationException();
    }

    @Override
    public final boolean hasKeys() {
        return !tags.isEmpty();
    }

    @Override
    public final Collection<String> keySet() {
        return Collections.unmodifiableSet(tags.keySet());
    }

    @Override
    public int getNumKeys() {
        return tags.size();
    }

    /**
     * Replies the key/value map.
     * @return the key/value map
     */
    public Map<String, String> getTags() {
        return Collections.unmodifiableMap(tags);
    }

    /**
     * Returns the changeset for this history primitive.
     * @return the changeset for this history primitive
     */
    public Changeset getChangeset() {
        return changeset;
    }

    /**
     * Sets the changeset for this history primitive.
     * @param changeset the changeset for this history primitive
     */
    public void setChangeset(Changeset changeset) {
        this.changeset = changeset;
    }

    /**
     * Sets the tags for this history primitive. Removes all tags if <code>tags</code> is null.
     *
     * @param tags the tags. May be null.
     */
    public void setTags(Map<String, String> tags) {
        if (tags == null) {
            this.tags = new HashMap<>();
        } else {
            this.tags = new HashMap<>(tags);
        }
    }

    /**
     * Replies the name of this primitive. The default implementation replies the value
     * of the tag <code>name</code> or null, if this tag is not present.
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
        String key = "name:" + Locale.getDefault();
        if (get(key) != null)
            return get(key);
        key = "name:" + Locale.getDefault().getLanguage() + '_' + Locale.getDefault().getCountry();
        if (get(key) != null)
            return get(key);
        key = "name:" + Locale.getDefault().getLanguage();
        if (get(key) != null)
            return get(key);
        return getName();
    }

    /**
     * Fills the attributes common to all primitives with values from this history.
     * @param data primitive data to fill
     */
    protected void fillPrimitiveCommonData(PrimitiveData data) {
        data.setUser(user);
        try {
            data.setVisible(visible);
        } catch (IllegalStateException e) {
            Logging.log(Logging.LEVEL_ERROR, "Cannot change visibility for "+data+':', e);
        }
        data.setTimestamp(timestamp);
        data.setKeys(tags);
        data.setOsmId(id, (int) version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, version);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        HistoryOsmPrimitive that = (HistoryOsmPrimitive) obj;
        return id == that.id && version == that.version;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [version=" + version + ", id=" + id + ", visible=" + visible + ", "
                + (timestamp != null ? ("timestamp=" + timestamp) : "") + ", "
                + (user != null ? ("user=" + user + ", ") : "") + "changesetId="
                + changesetId
                + ']';
    }
}
