// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * An enum representing the compression type of a resource.
 */
public enum Compression {
    /**
     * no compression
     */
    NONE,
    /**
     * bzip2 compression
     */
    BZIP2,
    /**
     * gzip compression
     */
    GZIP,
    /**
     * zip compression
     */
    ZIP;

    /**
     * Determines the compression type depending on the suffix of {@code name}.
     * @param name File name including extension
     * @return the compression type
     */
    public static Compression byExtension(String name) {
        return name != null && name.endsWith(".gz")
                ? GZIP
                : name != null && (name.endsWith(".bz2") || name.endsWith(".bz"))
                ? BZIP2
                : name != null && name.endsWith(".zip")
                ? ZIP
                : NONE;
    }

    /**
     * Determines the compression type based on the content type (MIME type).
     * @param contentType the content type
     * @return the compression type
     */
    public static Compression forContentType(String contentType) {
        switch (contentType) {
        case "application/zip":
            return ZIP;
        case "application/x-gzip":
            return GZIP;
        case "application/x-bzip2":
            return BZIP2;
        default:
            return NONE;
        }
    }

    /**
     * Returns an un-compressing {@link InputStream} for {@code in}.
     * @param in raw input stream
     * @return un-compressing input stream
     *
     * @throws IOException if any I/O error occurs
     */
    public InputStream getUncompressedInputStream(InputStream in) throws IOException {
        switch (this) {
            case BZIP2:
                return getBZip2InputStream(in);
            case GZIP:
                return getGZipInputStream(in);
            case ZIP:
                return getZipInputStream(in);
            case NONE:
            default:
                return in;
        }
    }

    /**
     * Returns a Bzip2 input stream wrapping given input stream.
     * @param in The raw input stream
     * @return a Bzip2 input stream wrapping given input stream, or {@code null} if {@code in} is {@code null}
     * @throws IOException if the given input stream does not contain valid BZ2 header
     * @since 12772 (moved from {@link Utils}, there since 7867)
     */
    public static BZip2CompressorInputStream getBZip2InputStream(InputStream in) throws IOException {
        if (in == null) {
            return null;
        }
        return new BZip2CompressorInputStream(in, /* see #9537 */ true);
    }

    /**
     * Returns a Gzip input stream wrapping given input stream.
     * @param in The raw input stream
     * @return a Gzip input stream wrapping given input stream, or {@code null} if {@code in} is {@code null}
     * @throws IOException if an I/O error has occurred
     * @since 12772 (moved from {@link Utils}, there since 7119)
     */
    public static GZIPInputStream getGZipInputStream(InputStream in) throws IOException {
        if (in == null) {
            return null;
        }
        return new GZIPInputStream(in);
    }

    /**
     * Returns a Zip input stream wrapping given input stream.
     * @param in The raw input stream
     * @return a Zip input stream wrapping given input stream, or {@code null} if {@code in} is {@code null}
     * @throws IOException if an I/O error has occurred
     * @since 12772 (moved from {@link Utils}, there since 7119)
     */
    public static ZipInputStream getZipInputStream(InputStream in) throws IOException {
        if (in == null) {
            return null;
        }
        ZipInputStream zis = new ZipInputStream(in, StandardCharsets.UTF_8);
        // Positions the stream at the beginning of first entry
        ZipEntry ze = zis.getNextEntry();
        if (ze != null && Logging.isDebugEnabled()) {
            Logging.debug("Zip entry: {0}", ze.getName());
        }
        return zis;
    }

    /**
     * Returns an un-compressing {@link InputStream} for the {@link File} {@code file}.
     * @param file file
     * @return un-compressing input stream
     * @throws IOException if any I/O error occurs
     */
    public static InputStream getUncompressedFileInputStream(File file) throws IOException {
        InputStream in = Files.newInputStream(file.toPath());
        try {
            return byExtension(file.getName()).getUncompressedInputStream(in);
        } catch (IOException e) {
            Utils.close(in);
            throw e;
        }
    }

    /**
     * Returns a compressing {@link OutputStream} for {@code out}.
     * @param out raw output stream
     * @return compressing output stream
     *
     * @throws IOException if any I/O error occurs
     */
    public OutputStream getCompressedOutputStream(OutputStream out) throws IOException {
        switch (this) {
            case BZIP2:
                return new BZip2CompressorOutputStream(out);
            case GZIP:
                return new GZIPOutputStream(out);
            case ZIP:
                return new ZipOutputStream(out, StandardCharsets.UTF_8);
            case NONE:
            default:
                return out;
        }
    }

    /**
     * Returns a compressing {@link OutputStream} for the {@link File} {@code file}.
     * @param file file
     * @return compressing output stream
     *
     * @throws IOException if any I/O error occurs
     */
    public static OutputStream getCompressedFileOutputStream(File file) throws IOException {
        OutputStream out = Files.newOutputStream(file.toPath());
        try {
            return byExtension(file.getName()).getCompressedOutputStream(out);
        } catch (IOException e) {
            Utils.close(out);
            throw e;
        }
    }
}
