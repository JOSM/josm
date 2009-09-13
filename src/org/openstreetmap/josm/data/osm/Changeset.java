// License: GPL. Copyright 2007 by Martijn van Oosterhout and others
package org.openstreetmap.josm.data.osm;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.visitor.Visitor;

/**
 * Represents a single changeset in JOSM. For now its only used during
 * upload but in the future we may do more.
 *
 */
public final class Changeset implements Tagged {
    /** the changeset id */
    private long id;
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


    /**
     * Creates a new changeset with id 0.
     */
    public Changeset() {
        this.id = 0;
        this.tags = new HashMap<String, String>();
    }

    /**
     * Creates a changeset with id <code>id</code>. If id > 0, sets incomplete to true.
     * 
     * @param id the id
     */
    public Changeset(long id) {
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
        } else {
            cloneFrom(other);
            this.incomplete = false;
        }
    }

    public void cloneFrom(Changeset other) {
        setId(other.getId());
        setUser(other.getUser());
        setCreatedAt(other.getCreatedAt());
        setClosedAt(other.getClosedAt());
        setMin(other.getMin());
        setMax(other.getMax());
        setKeys(other.getKeys());
        setOpen(other.isOpen());
    }

    public void visit(Visitor v) {
        v.visit(this);
    }

    public int compareTo(Changeset other) {
        return Long.valueOf(getId()).compareTo(other.getId());
    }

    public String getName() {
        // no translation
        return "changeset " + getId();
    }

    public String getLocalName(){
        return tr("Changeset {0}",getId());
    }

    public String getDisplayName(NameFormatter formatter) {
        return formatter.format(this);
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

    public void setMax(LatLon max) {
        this.max = max;
    }

    public Map<String, String> getKeys() {
        return tags;
    }

    public void setKeys(Map<String, String> keys) {
        this.tags = keys;
    }

    public boolean isIncomplete() {
        return incomplete;
    }

    public void setIncomplete(boolean incomplete) {
        this.incomplete = incomplete;
    }

    public void put(String key, String value) {
        this.tags.put(key, value);
    }

    public String get(String key) {
        return this.tags.get(key);
    }

    public void remove(String key) {
        this.tags.remove(key);
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
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (id ^ (id >>> 32));
        if (id > 0)
            return prime * result + getClass().hashCode();
        result = prime * result + ((closedAt == null) ? 0 : closedAt.hashCode());
        result = prime * result + ((createdAt == null) ? 0 : createdAt.hashCode());
        result = prime * result + ((max == null) ? 0 : max.hashCode());
        result = prime * result + ((min == null) ? 0 : min.hashCode());
        result = prime * result + (open ? 1231 : 1237);
        result = prime * result + ((tags == null) ? 0 : tags.hashCode());
        result = prime * result + ((user == null) ? 0 : user.hashCode());
        return result;
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

    public boolean hasKeys() {
        return !tags.keySet().isEmpty();
    }

    public Collection<String> keySet() {
        return tags.keySet();
    }
}
