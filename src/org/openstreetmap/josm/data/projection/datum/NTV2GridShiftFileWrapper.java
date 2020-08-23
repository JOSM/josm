// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.datum;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Wrapper for {@link NTV2GridShiftFile}.
 *
 * Loads the shift file from disk, when it is first accessed.
 * @since 5226
 */
public class NTV2GridShiftFileWrapper {

    private NTV2GridShiftFile instance;
    private final String gridFileName;

    /** Priority for local NTV2 grid files */
    public static final float NTV2_SOURCE_PRIORITY_LOCAL = 10f;
    /** Priority for downloaded NTV2 grid files */
    public static final float NTV2_SOURCE_PRIORITY_DOWNLOAD = 5f;

    private static final Map<Float, NTV2GridShiftFileSource> sources = new TreeMap<>(Collections.reverseOrder());

    /**
     * Register a source for NTV2 grid files.
     * @param priority the priority, sources with higher priority are checked first;
     * use {@link #NTV2_SOURCE_PRIORITY_LOCAL} for local files and
     * {@link #NTV2_SOURCE_PRIORITY_DOWNLOAD} for remote downloads
     * @param source the NTV2 grid file source
     * @since 12777
     */
    public static void registerNTV2GridShiftFileSource(float priority, NTV2GridShiftFileSource source) {
        sources.put(priority, source);
    }

    /**
     * Constructs a new {@code NTV2GridShiftFileWrapper}.
     * @param filename Path to the grid file (GSB format)
     */
    public NTV2GridShiftFileWrapper(String filename) {
        this.gridFileName = Objects.requireNonNull(filename);
    }

    /**
     * Returns the actual {@link NTV2GridShiftFile} behind this wrapper.
     * The grid file is only loaded once, when first accessed.
     * @return The NTv2 grid file
     * @throws IOException if the grid file cannot be found/loaded
     */
    public synchronized NTV2GridShiftFile getShiftFile() throws IOException {
        if (instance == null) {
            for (Map.Entry<Float, NTV2GridShiftFileSource> entry : sources.entrySet()) {
                NTV2GridShiftFileSource source = entry.getValue();
                try (InputStream is = source.getNTV2GridShiftFile(gridFileName)) {
                    if (is != null) {
                        NTV2GridShiftFile ntv2 = new NTV2GridShiftFile();
                        ntv2.loadGridShiftFile(is, false);
                        instance = ntv2;
                        break;
                    }
                }
            }
            if (instance == null) {
                throw new IOException("Unable to find NTV2 grid shift file for " + gridFileName);
            }
        }
        return instance;
    }
}
