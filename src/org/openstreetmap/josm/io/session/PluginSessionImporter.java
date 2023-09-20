// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.session;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Import arbitrary data for a plugin.
 * @since 18833
 */
public interface PluginSessionImporter {
    /**
     * Get the filename that was used to store data in the archive.
     * @return The filename
     * @see PluginSessionExporter#getFileName()
     */
    String getFileName();

    /**
     * Read data from a file stream
     * @param inputStream The stream to read
     * @return {@code true} if the importer loaded data
     */
    boolean read(InputStream inputStream);

    /**
     * Read the data from a zip file
     * @param zipFile The zipfile to read
     * @return {@code true} if the importer loaded data
     * @throws IOException if there was an issue reading the zip file. See {@link ZipFile#getInputStream(ZipEntry)}.
     */
    default boolean readZipFile(ZipFile zipFile) throws IOException {
        final ZipEntry entry = zipFile.getEntry(this.getFileName());
        if (entry != null) {
            try (InputStream inputStream = zipFile.getInputStream(entry)) {
                return this.read(inputStream);
            }
        }
        return false;
    }
}
