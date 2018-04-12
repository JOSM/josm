// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.date.DateUtils;

/**
 * Represents a single changeset in JOSM. For now its only used during
 * upload but in the future we may do more.
 * @since 625
 */
public final class Changeset implements Tagged, Comparable<Changeset> {

    /** The maximum changeset tag length allowed by API 0.6 **/
    public static final int MAX_CHANGESET_TAG_LENGTH = MAX_TAG_LENGTH;

    /** the changeset id */
    private int id;
    /** the user who owns the changeset */
    private User user;
    /** date this changeset was created at */
    private Date createdAt;
    /** the date this changeset was closed at*/
    private Date closedAt;
    /** indicates whether this changeset is still open or not */
    private boolean open;
    /** the min. coordinates of the bounding box of this changeset */
    private LatLon min;
    /** the max. coordinates of the bounding box of this changeset */
    private LatLon max;
    /** the number of comments for this changeset */
    private int commentsCount;
    /** the map of tags */
    private Map<String, String> tags;
    /** indicates whether this changeset is incomplete. For an incomplete changeset we only know its id */
    private boolean incomplete;
    /** the changeset content */
    private ChangesetDataSet content;
    /** the changeset discussion */
    private List<ChangesetDiscussionComment> discussion;

    /**
     * Creates a new changeset with id 0.
     */
    public Changeset() {
        this(0);
    }

    /**
     * Creates a changeset with id <code>id</code>. If id &gt; 0, sets incomplete to true.
     *
     * @param id the id
     */
    public Changeset(int id) {
        this.id = id;
        this.incomplete = id > 0;
        this.tags = new HashMap<>();
    }

    /**
     * Creates a clone of <code>other</code>
     *
     * @param other the other changeset. If null, creates a new changeset with id 0.
     */
    public Changeset(Changeset other) {
        if (other == null) {
            this.id = 0;
            this.tags = new HashMap<>();
        } else if (other.isIncomplete()) {
            setId(other.getId());
            this.incomplete = true;
            this.tags = new HashMap<>();
        } else {
            this.id = other.id;
            mergeFrom(other);
            this.incomplete = false;
        }
    }

    /**
     * Creates a changeset with the data obtained from the given preset, i.e.,
     * the {@link AbstractPrimitive#getChangesetId() changeset id}, {@link AbstractPrimitive#getUser() user}, and
     * {@link AbstractPrimitive#getTimestamp() timestamp}.
     * @param primitive the primitive to use
     * @return the created changeset
     */
    public static Changeset fromPrimitive(final OsmPrimitive primitive) {
        final Changeset changeset = new Changeset(primitive.getChangesetId());
        changeset.setUser(primitive.getUser());
        changeset.setCreatedAt(primitive.getTimestamp()); // not accurate in all cases
        return changeset;
    }

    /**
     * Compares this changeset to another, based on their identifier.
     * @param other other changeset
     * @return the value {@code 0} if {@code getId() == other.getId()};
     *         a value less than {@code 0} if {@code getId() < other.getId()}; and
     *         a value greater than {@code 0} if {@code getId() > other.getId()}
     */
    @Override
    public int compareTo(Changeset other) {
        return Integer.compare(getId(), other.getId());
    }

    /**
     * Returns the changeset name.
     * @return the changeset name (untranslated: "changeset &lt;identifier&gt;")
     */
    public String getName() {
        // no translation
        return "changeset " + getId();
    }

    /**
     * Returns the changeset display name, as per given name formatter.
     * @param formatter name formatter
     * @return the changeset display name, as per given name formatter
     */
    public String getDisplayName(NameFormatter formatter) {
        return formatter.format(this);
    }

    /**
     * Returns the changeset identifier.
     * @return the changeset identifier
     */
    public int getId() {
        return id;
    }

    /**
     * Sets the changeset identifier.
     * @param id changeset identifier
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Returns the changeset user.
     * @return the changeset user
     */
    public User getUser() {
        return user;
    }

    /**
     * Sets the changeset user.
     * @param user changeset user
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * Returns the changeset creation date.
     * @return the changeset creation date
     */
    public Date getCreatedAt() {
        return DateUtils.cloneDate(createdAt);
    }

    /**
     * Sets the changeset creation date.
     * @param createdAt changeset creation date
     */
    public void setCreatedAt(Date createdAt) {
        this.createdAt = DateUtils.cloneDate(createdAt);
    }

    /**
     * Returns the changeset closure date.
     * @return the changeset closure date
     */
    public Date getClosedAt() {
        return DateUtils.cloneDate(closedAt);
    }

    /**
     * Sets the changeset closure date.
     * @param closedAt changeset closure date
     */
    public void setClosedAt(Date closedAt) {
        this.closedAt = DateUtils.cloneDate(closedAt);
    }

    /**
     * Determines if this changeset is open.
     * @return {@code true} if this changeset is open
     */
    public boolean isOpen() {
        return open;
    }

    /**
     * Sets whether this changeset is open.
     * @param open {@code true} if this changeset is open
     */
    public void setOpen(boolean open) {
        this.open = open;
    }

    /**
     * Returns the min lat/lon of the changeset bounding box.
     * @return the min lat/lon of the changeset bounding box
     */
    public LatLon getMin() {
        return min;
    }

    /**
     * Sets the min lat/lon of the changeset bounding box.
     * @param min min lat/lon of the changeset bounding box
     */
    public void setMin(LatLon min) {
        this.min = min;
    }

    /**
     * Returns the max lat/lon of the changeset bounding box.
     * @return the max lat/lon of the changeset bounding box
     */
    public LatLon getMax() {
        return max;
    }

    /**
     * Sets the max lat/lon of the changeset bounding box.
     * @param max min lat/lon of the changeset bounding box
     */
    public void setMax(LatLon max) {
        this.max = max;
    }

    /**
     * Returns the changeset bounding box.
     * @return the changeset bounding box
     */
    public Bounds getBounds() {
        if (min != null && max != null)
            return new Bounds(min, max);
        return null;
    }

    /**
     * Replies this changeset comment.
     * @return this changeset comment (empty string if missing)
     * @since 12494
     */
    public String getComment() {
        return Optional.ofNullable(get("comment")).orElse("");
    }

    /**
     * Replies the number of comments for this changeset discussion.
     * @return the number of comments for this changeset discussion
     * @since 7700
     */
    public int getCommentsCount() {
        return commentsCount;
    }

    /**
     * Sets the number of comments for this changeset discussion.
     * @param commentsCount the number of comments for this changeset discussion
     * @since 7700
     */
    public void setCommentsCount(int commentsCount) {
        this.commentsCount = commentsCount;
    }

    @Override
    public Map<String, String> getKeys() {
        return tags;
    }

    @Override
    public void setKeys(Map<String, String> keys) {
        CheckParameterUtil.ensureParameterNotNull(keys, "keys");
        keys.values().stream()
                .filter(value -> value != null && value.length() > MAX_CHANGESET_TAG_LENGTH)
                .findFirst()
                .ifPresent(value -> {
                throw new IllegalArgumentException("Changeset tag value is too long: "+value);
        });
        this.tags = keys;
    }

    /**
     * Determines if this changeset is incomplete.
     * @return {@code true} if this changeset is incomplete
     */
    public boolean isIncomplete() {
        return incomplete;
    }

    /**
     * Sets whether this changeset is incomplete
     * @param incomplete {@code true} if this changeset is incomplete
     */
    public void setIncomplete(boolean incomplete) {
        this.incomplete = incomplete;
    }

    @Override
    public void put(String key, String value) {
        CheckParameterUtil.ensureParameterNotNull(key, "key");
        if (value != null && value.length() > MAX_CHANGESET_TAG_LENGTH) {
            throw new IllegalArgumentException("Changeset tag value is too long: "+value);
        }
        this.tags.put(key, value);
    }

    @Override
    public String get(String key) {
        return this.tags.get(key);
    }

    @Override
    public void remove(String key) {
        this.tags.remove(key);
    }

    @Override
    public void removeAll() {
        this.tags.clear();
    }

    /**
     * Determines if this changeset has equals semantic attributes with another one.
     * @param other other changeset
     * @return {@code true} if this changeset has equals semantic attributes with other changeset
     */
    public boolean hasEqualSemanticAttributes(Changeset other) {
        if (other == null)
            return false;
        if (closedAt == null) {
            if (other.closedAt != null)
                return false;
        } else if (!closedAt.equals(other.closedAt))
            return false;
        if (createdAt == null) {
            if (other.createdAt != null)
                return false;
        } else if (!createdAt.equals(other.createdAt))
            return false;
        if (id != other.id)
            return false;
        if (max == null) {
            if (other.max != null)
                return false;
        } else if (!max.equals(other.max))
            return false;
        if (min == null) {
            if (other.min != null)
                return false;
        } else if (!min.equals(other.min))
            return false;
        if (open != other.open)
            return false;
        if (!tags.equals(other.tags))
            return false;
        if (user == null) {
            if (other.user != null)
                return false;
        } else if (!user.equals(other.user))
            return false;
        return commentsCount == other.commentsCount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Changeset changeset = (Changeset) obj;
        return id == changeset.id;
    }

    @Override
    public boolean hasKeys() {
        return !tags.keySet().isEmpty();
    }

    @Override
    public Collection<String> keySet() {
        return tags.keySet();
    }

    @Override
    public int getNumKeys() {
        return tags.size();
    }

    /**
     * Determines if this changeset is new.
     * @return {@code true} if this changeset is new ({@code id <= 0})
     */
    public boolean isNew() {
        return id <= 0;
    }

    /**
     * Merges changeset metadata from another changeset.
     * @param other other changeset
     */
    public void mergeFrom(Changeset other) {
        if (other == null)
            return;
        if (id != other.id)
            return;
        this.user = other.user;
        this.createdAt = DateUtils.cloneDate(other.createdAt);
        this.closedAt = DateUtils.cloneDate(other.closedAt);
        this.open = other.open;
        this.min = other.min;
        this.max = other.max;
        this.commentsCount = other.commentsCount;
        this.tags = new HashMap<>(other.tags);
        this.incomplete = other.incomplete;
        this.discussion = other.discussion != null ? new ArrayList<>(other.discussion) : null;

        // FIXME: merging of content required?
        this.content = other.content;
    }

    /**
     * Determines if this changeset has contents.
     * @return {@code true} if this changeset has contents
     */
    public boolean hasContent() {
        return content != null;
    }

    /**
     * Returns the changeset contents.
     * @return the changeset contents, can be null
     */
    public ChangesetDataSet getContent() {
        return content;
    }

    /**
     * Sets the changeset contents.
     * @param content changeset contents, can be null
     */
    public void setContent(ChangesetDataSet content) {
        this.content = content;
    }

    /**
     * Replies the list of comments in the changeset discussion, if any.
     * @return the list of comments in the changeset discussion. May be empty but never null
     * @since 7704
     */
    public synchronized List<ChangesetDiscussionComment> getDiscussion() {
        if (discussion == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(discussion);
    }

    /**
     * Adds a comment to the changeset discussion.
     * @param comment the comment to add. Ignored if null
     * @since 7704
     */
    public synchronized void addDiscussionComment(ChangesetDiscussionComment comment) {
        if (comment == null) {
            return;
        }
        if (discussion == null) {
            discussion = new ArrayList<>();
        }
        discussion.add(comment);
    }
}
