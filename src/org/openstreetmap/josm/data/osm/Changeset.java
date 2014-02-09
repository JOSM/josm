// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.visitor.Visitor;

/**
 * Represents a single changeset in JOSM. For now its only used during
 * upload but in the future we may do more.
 *
 */
public final class Changeset implements Tagged {

    /** The maximum changeset comment text length allowed by API 0.6 **/
    public static final int MAX_COMMENT_LENGTH = 255;

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
    /** the map of tags */
    private Map<String,String> tags;
    /** indicates whether this changeset is incomplete. For an
     * incomplete changeset we only know its id
     */
    private boolean incomplete;
    /** the changeset content */
    private ChangesetDataSet content = null;

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
        this.tags = new HashMap<String, String>();
    }

    /**
     * Creates a clone of <code>other</code>
     *
     * @param other the other changeset. If null, creates a new changeset with id 0.
     */
    public Changeset(Changeset other) {
        if (other == null) {
            this.id = 0;
            this.tags = new HashMap<String, String>();
        } else if (other.isIncomplete()) {
            setId(other.getId());
            this.incomplete = true;
            this.tags = new HashMap<String, String>();
        } else {
            this.id = other.id;
            mergeFrom(other);
            this.incomplete = false;
        }
    }

    public void visit(Visitor v) {
        v.visit(this);
    }

    public int compareTo(Changeset other) {
        return Integer.valueOf(getId()).compareTo(other.getId());
    }

    public String getName() {
        // no translation
        return "changeset " + getId();
    }

    public String getDisplayName(NameFormatter formatter) {
        return formatter.format(this);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Date closedAt) {
        this.closedAt = closedAt;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public LatLon getMin() {
        return min;
    }

    public void setMin(LatLon min) {
        this.min = min;
    }

    public LatLon getMax() {
        return max;
    }

    public Bounds getBounds() {
        if (min != null && max != null)
            return new Bounds(min,max);
        return null;
    }

    public void setMax(LatLon max) {
        this.max = max;
    }

    @Override
    public Map<String, String> getKeys() {
        return tags;
    }

    @Override
    public void setKeys(Map<String, String> keys) {
        this.tags = keys;
    }

    public boolean isIncomplete() {
        return incomplete;
    }

    public void setIncomplete(boolean incomplete) {
        this.incomplete = incomplete;
    }

    @Override
    public void put(String key, String value) {
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
        if (tags == null) {
            if (other.tags != null)
                return false;
        } else if (!tags.equals(other.tags))
            return false;
        if (user == null) {
            if (other.user != null)
                return false;
        } else if (!user.equals(other.user))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        if (id > 0)
            return id;
        else
            return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Changeset other = (Changeset) obj;
        if (this.id > 0 && other.id == this.id)
            return true;
        return this == obj;
    }

    @Override
    public boolean hasKeys() {
        return !tags.keySet().isEmpty();
    }

    @Override
    public Collection<String> keySet() {
        return tags.keySet();
    }

    public boolean isNew() {
        return id <= 0;
    }

    public void mergeFrom(Changeset other) {
        if (other == null)
            return;
        if (id != other.id)
            return;
        this.user = other.user;
        this.createdAt = other.createdAt;
        this.closedAt = other.closedAt;
        this.open  = other.open;
        this.min = other.min;
        this.max = other.max;
        this.tags = new HashMap<String, String>(other.tags);
        this.incomplete = other.incomplete;

        // FIXME: merging of content required?
        this.content = other.content;
    }

    public boolean hasContent() {
        return content != null;
    }

    public ChangesetDataSet getContent() {
        return content;
    }

    public void setContent(ChangesetDataSet content) {
        this.content = content;
    }
}
