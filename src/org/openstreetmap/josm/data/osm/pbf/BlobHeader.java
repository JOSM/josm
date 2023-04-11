// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.pbf;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A "BlobHeader" which contains metadata for a {@link Blob}.
 * @since 18695
 */
public final class BlobHeader {
    private final String type;
    private final byte[] indexData;
    private final int dataSize;

    /**
     * Create a new {@link BlobHeader}
     * @param type The type of data in the blob
     * @param indexData The metadata for the blob
     * @param dataSize The size of the blob
     */
    public BlobHeader(@Nonnull String type, @Nullable byte[] indexData, int dataSize) {
        this.type = type;
        this.indexData = indexData;
        this.dataSize = dataSize;
    }

    /**
     * Get the blob type
     * @return The blob type
     */
    public String type() {
        return this.type;
    }

    /**
     * Get the blob metadata
     * @return The blob metadata
     */
    public byte[] indexData() {
        return this.indexData;
    }

    /**
     * Get the blob size
     * @return The blob size
     */
    public int dataSize() {
        return this.dataSize;
    }
}
