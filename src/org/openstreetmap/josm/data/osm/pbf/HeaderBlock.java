// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.pbf;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.openstreetmap.josm.data.osm.BBox;

/**
 * The header block contains data on required features, optional features, the bbox of the data, the source, the osmosis replication timestamp,
 * the osmosis replication sequence number, and the osmosis replication base url
 * @since 18695
 */
public final class HeaderBlock {
    private final BBox bbox;
    private final String[] requiredFeatures;
    private final String[] optionalFeatures;
    private final String writingProgram;
    private final String source;
    private final Long osmosisReplicationTimestamp;
    private final Long osmosisReplicationSequenceNumber;
    private final String osmosisReplicationBaseUrl;

    /**
     * Create a new {@link HeaderBlock} for an OSM PBF file
     * @param bbox The bbox
     * @param requiredFeatures The required features
     * @param optionalFeatures The optional features
     * @param writingProgram The program used to write the file
     * @param source The source
     * @param osmosisReplicationTimestamp The last time that osmosis updated the source (in seconds since epoch)
     * @param osmosisReplicationSequenceNumber The replication sequence number
     * @param osmosisReplicationBaseUrl The replication base url
     */
    public HeaderBlock(@Nullable BBox bbox, @Nonnull String[] requiredFeatures, @Nonnull String[] optionalFeatures,
                       @Nullable String writingProgram, @Nullable String source, @Nullable Long osmosisReplicationTimestamp,
                       @Nullable Long osmosisReplicationSequenceNumber, @Nullable String osmosisReplicationBaseUrl) {
        this.bbox = bbox;
        this.requiredFeatures = requiredFeatures;
        this.optionalFeatures = optionalFeatures;
        this.writingProgram = writingProgram;
        this.source = source;
        this.osmosisReplicationTimestamp = osmosisReplicationTimestamp;
        this.osmosisReplicationSequenceNumber = osmosisReplicationSequenceNumber;
        this.osmosisReplicationBaseUrl = osmosisReplicationBaseUrl;
    }

    /**
     * The required features to parse the PBF
     * @return The required features
     */
    @Nonnull
    public String[] requiredFeatures() {
        return this.requiredFeatures.clone();
    }

    /**
     * The optional features to parse the PBF
     * @return The optional features
     */
    @Nonnull
    public String[] optionalFeatures() {
        return this.optionalFeatures.clone();
    }

    /**
     * Get the program used to write the PBF
     * @return The program that wrote the PBF
     */
    @Nullable
    public String writingProgram() {
        return this.writingProgram;
    }

    /**
     * The source
     * @return The source (same as bbox field from OSM)
     */
    @Nullable
    public String source() {
        return this.source;
    }

    /**
     * The replication timestamp
     * @return The time that the file was last updated
     */
    @Nullable
    public Long osmosisReplicationTimestamp() {
        return this.osmosisReplicationTimestamp;
    }

    /**
     * The replication sequence number
     * @return The sequence number
     */
    @Nullable
    public Long osmosisReplicationSequenceNumber() {
        return this.osmosisReplicationSequenceNumber;
    }

    /**
     * The replication base URL
     * @return the base url for replication, if we ever want/need to continue the replication
     */
    @Nullable
    public String osmosisReplicationBaseUrl() {
        return this.osmosisReplicationBaseUrl;
    }

    /**
     * The bbox
     * @return The bbox
     */
    @Nullable
    public BBox bbox() {
        return this.bbox;
    }
}
