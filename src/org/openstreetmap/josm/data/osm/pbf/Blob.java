// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.pbf;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.InflaterInputStream;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * A "Blob" of data from an OSM PBF file. It, in turn, contains additional data in PBF format, which may be compressed.
 * @since 18695
 */
public final class Blob {
    /** The compression types for the blob */
    public enum CompressionType {
        /** No compression */
        raw,
        /** zlib compression */
        zlib,
        /** lzma compression (optional) */
        lzma,
        /** bzip2 compression (deprecated in 2010, so if we ever support saving PBF files, <i>don't use this compression type</i>) */
        bzip2,
        /** lz4 compression (optional) */
        lz4,
        /** zstd compression (optional) */
        zstd
    }

    private final Integer rawSize;
    private final CompressionType compressionType;
    private final byte[] bytes;

    /**
     * Create a new blob
     * @param rawSize The blob size
     * @param compressionType The compression type
     * @param bytes The bytes of the blob
     */
    public Blob(@Nullable Integer rawSize, @Nonnull CompressionType compressionType, @Nonnull byte... bytes) {
        this.rawSize = rawSize;
        this.compressionType = compressionType;
        this.bytes = bytes;
    }

    /**
     * The raw size of the blob (after decompression)
     * @return The raw size
     */
    @Nullable
    public Integer rawSize() {
        return this.rawSize;
    }

    /**
     * The compression type of the blob
     * @return The compression type
     */
    @Nonnull
    public CompressionType compressionType() {
        return this.compressionType;
    }

    /**
     * The bytes that make up the blob data
     * @return The bytes
     */
    @Nonnull
    public byte[] bytes() {
        return this.bytes;
    }

    /**
     * Get the decompressed inputstream for this blob
     * @return The decompressed inputstream
     * @throws IOException if we don't support the compression type <i>or</i> the decompressor has issues, see
     * <ul>
     *     <li>{@link BZip2CompressorInputStream}</li>
     * </ul>
     */
    @Nonnull
    public InputStream inputStream() throws IOException {
        final ByteArrayInputStream bais = new ByteArrayInputStream(this.bytes);
        switch (this.compressionType) {
            case raw:
                return bais;
            case bzip2:
                return new BZip2CompressorInputStream(bais);
            case lzma:
            case zstd:
            case lz4:
                throw new IOException(this.compressionType + " pbf is not currently supported");
            case zlib:
                return new InflaterInputStream(bais);
            default:
                throw new IOException("unknown compression type is not currently supported: " + this.compressionType.name());
        }
    }
}
