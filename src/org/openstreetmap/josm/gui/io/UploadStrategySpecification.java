// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

public class UploadStrategySpecification {
    static public final int UNSPECIFIED_CHUNK_SIZE = -1;
    static public UploadStrategySpecification createIndividualObjectStrategy() {
        return new UploadStrategySpecification(UploadStrategy.INDIVIDUAL_OBJECTS_STRATEGY, 1);
    }

    static public UploadStrategySpecification createChunkedUploadStrategy(int chunkSize) {
        return new UploadStrategySpecification(UploadStrategy.CHUNKED_DATASET_STRATEGY, chunkSize);
    }

    static public UploadStrategySpecification createSingleRequestUploadStrategy() {
        return new UploadStrategySpecification(UploadStrategy.SINGLE_REQUEST_STRATEGY, UNSPECIFIED_CHUNK_SIZE);
    }

    private UploadStrategy strategy;
    private int chunkSize;

    private UploadStrategySpecification(UploadStrategy strategy, int chunkSize) {
        this.strategy = strategy;
        this.chunkSize = chunkSize;
    }

    public UploadStrategy getStrategy() {
        return strategy;
    }
    public int getChunkSize() {
        return chunkSize;
    }
}
