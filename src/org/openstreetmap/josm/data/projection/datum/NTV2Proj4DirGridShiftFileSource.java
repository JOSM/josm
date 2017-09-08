// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.datum;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Platform;
import org.openstreetmap.josm.tools.PlatformVisitor;

/**
 * Shift file source that scans the common data directories of the proj4 library.
 * @since 12777
 */
public final class NTV2Proj4DirGridShiftFileSource implements NTV2GridShiftFileSource, PlatformVisitor<List<File>> {

    private NTV2Proj4DirGridShiftFileSource() {
        // hide constructor
    }

    // lazy initialization
    private static class InstanceHolder {
        static final NTV2Proj4DirGridShiftFileSource INSTANCE = new NTV2Proj4DirGridShiftFileSource();
    }

    /**
     * Get the singleton instance of this class.
     * @return the singleton instance of this class
     */
    public static NTV2Proj4DirGridShiftFileSource getInstance() {
        return InstanceHolder.INSTANCE;
    }

    @Override
    public InputStream getNTV2GridShiftFile(String gridFileName) {
        File grid = null;
        // Check is the grid is installed in default PROJ.4 directories
        for (File dir : Platform.determinePlatform().accept(this)) {
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
        if (grid != null) {
            try {
                return new FileInputStream(grid.getAbsoluteFile());
            } catch (FileNotFoundException ex) {
                Logging.warn("NTV2 grid shift file not found: " + grid);
            }
        }
        return null;
    }

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
}
