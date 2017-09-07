// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.datum;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.tools.Platform;
import org.openstreetmap.josm.tools.PlatformVisitor;

/**
 * Wrapper for {@link NTV2GridShiftFile}.
 *
 * Loads the shift file from disk, when it is first accessed.
 * @since 5226
 */
public class NTV2GridShiftFileWrapper {

    private NTV2GridShiftFile instance;
    private final String gridFileName;

    /**
     * Constructs a new {@code NTV2GridShiftFileWrapper}.
     * @param filename Path to the grid file (GSB format)
     */
    public NTV2GridShiftFileWrapper(String filename) {
        this.gridFileName = filename;
    }

    /**
     * Lists default directories where the ntv2 shift files (NAD) for the proj4
     * library would be located on different platforms.
     */
    public static final PlatformVisitor<List<File>> DEFAULT_PROJ4_NTV2_SHIFT_DIRS =
            new PlatformVisitor<List<File>>() {
        @Override
        public List<File> visitUnixoid() {
            return Arrays.asList(new File("/usr/local/share/proj"), new File("/usr/share/proj"));
        }

        @Override
        public List<File> visitWindows() {
            return Arrays.asList(new File("C:\\PROJ\\NAD"));
        }

        @Override
        public List<File> visitOsx() {
            return Collections.emptyList();
        }
    };

    /**
     * Returns the actual {@link NTV2GridShiftFile} behind this wrapper.
     * The grid file is only loaded once, when first accessed.
     * @return The NTv2 grid file
     * @throws IOException if the grid file cannot be found/loaded
     */
    public synchronized NTV2GridShiftFile getShiftFile() throws IOException {
        if (instance == null) {
            File grid = null;
            // Check is the grid is installed in default PROJ.4 directories
            for (File dir : Platform.determinePlatform().accept(DEFAULT_PROJ4_NTV2_SHIFT_DIRS)) {
                File file = new File(dir, gridFileName);
                if (file.exists() && file.isFile()) {
                    grid = file;
                    break;
                }
            }
            // If not, search into PROJ_LIB directory
            if (grid == null) {
                String projLib = System.getProperty("PROJ_LIB");
                if (projLib != null && !projLib.isEmpty()) {
                    File dir = new File(projLib);
                    if (dir.exists() && dir.isDirectory()) {
                        File file = new File(dir, gridFileName);
                        if (file.exists() && file.isFile()) {
                            grid = file;
                        }
                    }
                }
            }
            // If not, retrieve it from JOSM website
            String location = grid != null ? grid.getAbsolutePath() : (Main.getJOSMWebsite() + "/proj/" + gridFileName);
            // Try to load grid file
            try (CachedFile cf = new CachedFile(location); InputStream is = cf.getInputStream()) {
                NTV2GridShiftFile ntv2 = new NTV2GridShiftFile();
                ntv2.loadGridShiftFile(is, false);
                instance = ntv2;
            }
        }
        return instance;
    }
}
