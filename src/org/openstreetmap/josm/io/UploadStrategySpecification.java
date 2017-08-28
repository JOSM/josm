// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

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
 * @since 12687 (moved from {@code gui.io} package)
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

    /**
     * Gets the chunk size
     * @return The max size of each upload chunk
     */
    public int getChunkSize() {
        return chunkSize;
    }

    /**
     * Gets a special value that is used to indicate that the chunk size was not specified
     * @return A special integer
     */
    public static int getUnspecifiedChunkSize() {
        return UNSPECIFIED_CHUNK_SIZE;
    }

    /**
     * Gets the policy that is used when the server max changeset size is exceeded.
     * @return What to do when the changeset size is exceeded
     */
    public MaxChangesetSizeExceededPolicy getPolicy() {
        return policy;
    }

    /**
     * Sets the upload strategy (chunk mode)
     * @param strategy The upload strategy
     * @return This object, for easy chaining
     */
    public UploadStrategySpecification setStrategy(UploadStrategy strategy) {
        this.strategy = strategy;
        return this;
    }

    /**
     * Sets the upload chunk size
     * @param chunkSize The chunk size
     * @return This object, for easy chaining
     */
    public UploadStrategySpecification setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
        return this;
    }

    /**
     * Sets the policy to use when the max changeset size is exceeded
     * @param policy The policy
     * @return This object, for easy chaining
     */
    public UploadStrategySpecification setPolicy(MaxChangesetSizeExceededPolicy policy) {
        this.policy = policy;
        return this;
    }

    /**
     * Sets whether to close the changeset after this upload
     * @param closeChangesetAfterUpload <code>true</code> to close it
     * @return This object, for easy chaining
     */
    public UploadStrategySpecification setCloseChangesetAfterUpload(boolean closeChangesetAfterUpload) {
        this.closeChangesetAfterUpload = closeChangesetAfterUpload;
        return this;
    }

    /**
     * Gets if the changeset should be closed after this upload
     * @return <code>true</code> to close it
     */
    public boolean isCloseChangesetAfterUpload() {
        return closeChangesetAfterUpload;
    }

    /**
     * Gets the number of requests that will be required to upload the objects
     * @param numObjects The number of objects
     * @return The number of requests
     */
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
