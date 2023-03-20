// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.pbf;

import javax.annotation.Nullable;

/**
 * Optional metadata for primitives
 * @since 18695
 */
public final class Info {
    private final int version;
    private final Long timestamp;
    private final Long changeset;
    private final Integer uid;
    private final Integer userSid;
    private final boolean visible;

    /**
     * Create a new {@link Info} object
     * @param version The OSM version
     * @param timestamp The OSM timestamp
     * @param changeset The OSM changeset
     * @param uid The user ID
     * @param userSid The string id for the user name
     * @param visible {@code false} if the element was deleted for this version
     */
    public Info(int version, @Nullable Long timestamp, @Nullable Long changeset, @Nullable Integer uid, @Nullable Integer userSid,
                boolean visible) {
        this.version = version;
        this.timestamp = timestamp;
        this.changeset = changeset;
        this.uid = uid;
        this.userSid = userSid;
        this.visible = visible;
    }

    /**
     * @return The version
     */
    public int version() {
        return this.version;
    }

    /**
     * @return The timestamp
     */
    public Long timestamp() {
        return this.timestamp;
    }

    /**
     * @return The changeset
     */
    public Long changeset() {
        return this.changeset;
    }

    /**
     * @return The user id
     */
    public Integer uid() {
        return this.uid;
    }

    /**
     * @return The user string id (in PBF strings)
     */
    public Integer userSid() {
        return this.userSid;
    }

    /**
     * @return {@code false} if the object was deleted
     */
    public boolean isVisible() {
        return this.visible;
    }
}
