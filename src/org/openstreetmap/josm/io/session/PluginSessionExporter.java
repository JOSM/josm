// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.session;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

/**
 * Export arbitrary data from a plugin.
 * @since 18833
 */
public interface PluginSessionExporter {
    /**
     * Get the filename to store the data in the archive
     * @return The filename
     * @see PluginSessionImporter#getFileName()
     */
    String getFileName();

    /**
     * Check to see if the specified exporter needs to save anything
     * @return {@code true} if the exporter needs to save something
     */
    boolean requiresSaving();

    /**
     * Write data to a zip file
     * @param zipOut The zip output stream
     * @throws IOException see {@link ZipOutputStream#putNextEntry(ZipEntry)}
     * @throws ZipException see {@link ZipOutputStream#putNextEntry(ZipEntry)}
     */
    default void writeZipEntries(ZipOutputStream zipOut) throws IOException {
        if (requiresSaving()) {
            final ZipEntry zipEntry = new ZipEntry(this.getFileName());
            zipOut.putNextEntry(zipEntry);
            this.write(zipOut);
        }
    }

    /**
     * Write the plugin data to a stream
     * @param outputStream The stream to write to
     */
    void write(OutputStream outputStream);
}
