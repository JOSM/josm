// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import java.util.Objects;

/**
 * An UploadStrategySpecification consists of the parameter describing the strategy
 * for uploading a collection of {@link org.openstreetmap.josm.data.osm.OsmPrimitive}.
 *
 * This includes:
 * <ul>
 * <li>a decision on which {@link UploadStrategy} to use</li>
 * <li>the upload chunk size</li>
 * <li>whether to close the changeset used after the upload</li>
 * </ul>
 */
public class UploadStrategySpecification {
    /** indicates that the chunk size isn't specified */
    public static final int UNSPECIFIED_CHUNK_SIZE = -1;

    private UploadStrategy strategy;
    private int chunkSize;
    private MaxChangesetSizeExceededPolicy policy;
    private boolean closeChangesetAfterUpload;

    /**
     * Creates a new upload strategy with default values.
     */
    public UploadStrategySpecification() {
        this.strategy = UploadStrategy.DEFAULT_UPLOAD_STRATEGY;
        this.chunkSize = UNSPECIFIED_CHUNK_SIZE;
        this.policy = null;
        this.closeChangesetAfterUpload = true;
    }

    /**
     * Clones another upload strategy. If other is null, assumes default values.
     *
     * @param other the other upload strategy
     */
    public UploadStrategySpecification(UploadStrategySpecification other) {
        if (other != null) {
            this.strategy = other.strategy;
            this.chunkSize = other.chunkSize;
            this.policy = other.policy;
            this.closeChangesetAfterUpload = other.closeChangesetAfterUpload;
        }
    }

    /**
     * Replies the upload strategy
     * @return the upload strategy
     */
    public UploadStrategy getStrategy() {
        return strategy;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public static int getUnspecifiedChunkSize() {
        return UNSPECIFIED_CHUNK_SIZE;
    }

    public MaxChangesetSizeExceededPolicy getPolicy() {
        return policy;
    }

    public UploadStrategySpecification setStrategy(UploadStrategy strategy) {
        this.strategy = strategy;
        return this;
    }

    public UploadStrategySpecification setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
        return this;
    }

    public UploadStrategySpecification setPolicy(MaxChangesetSizeExceededPolicy policy) {
        this.policy = policy;
        return this;
    }

    public UploadStrategySpecification setCloseChangesetAfterUpload(boolean closeChangesetAfterUpload) {
        this.closeChangesetAfterUpload = closeChangesetAfterUpload;
        return this;
    }

    public boolean isCloseChangesetAfterUpload() {
        return closeChangesetAfterUpload;
    }

    public int getNumRequests(int numObjects) {
        if (numObjects <= 0)
            return 0;
        switch(strategy) {
        case INDIVIDUAL_OBJECTS_STRATEGY: return numObjects;
        case SINGLE_REQUEST_STRATEGY: return 1;
        case CHUNKED_DATASET_STRATEGY:
            if (chunkSize == UNSPECIFIED_CHUNK_SIZE)
                return 0;
            else
                return (int) Math.ceil((double) numObjects / (double) chunkSize);
        }
        // should not happen
        return 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(strategy, chunkSize, policy, closeChangesetAfterUpload);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        UploadStrategySpecification that = (UploadStrategySpecification) obj;
        return chunkSize == that.chunkSize &&
                closeChangesetAfterUpload == that.closeChangesetAfterUpload &&
                strategy == that.strategy &&
                policy == that.policy;
    }
}
