// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.pbf;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A "BlobHeader" which contains metadata for a {@link Blob}.
 * @since xxx
 */
public final class BlobHeader {
    private final String type;
    private final byte[] indexData;
    private final int dataSize;

    public BlobHeader(@Nonnull String type, @Nullable byte[] indexData, int dataSize) {
        this.type = type;
        this.indexData = indexData;
        this.dataSize = dataSize;
    }

    public String type() {
        return this.type;
    }

    public byte[] indexData() {
        return this.indexData;
    }

    public int dataSize() {
        return this.dataSize;
    }
}
