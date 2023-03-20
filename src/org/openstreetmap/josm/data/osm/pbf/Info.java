// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.pbf;

import javax.annotation.Nullable;

/**
 * Optional metadata for primitives
 * @since xxx
 */
public final class Info {
    private final int version;
    private final Long timestamp;
    private final Long changeset;
    private final Integer uid;
    private final Integer userSid;
    private final boolean visible;

    public Info(int version, @Nullable Long timestamp, @Nullable Long changeset, @Nullable Integer uid, @Nullable Integer userSid,
                boolean visible) {
        this.version = version;
        this.timestamp = timestamp;
        this.changeset = changeset;
        this.uid = uid;
        this.userSid = userSid;
        this.visible = visible;
    }

    public int version() {
        return this.version;
    }

    public Long timestamp() {
        return this.timestamp;
    }

    public Long changeset() {
        return this.changeset;
    }

    public Integer uid() {
        return this.uid;
    }

    public Integer userSid() {
        return this.userSid;
    }

    public boolean isVisible() {
        return this.visible;
    }
}
