// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.datum;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Platform;
import org.openstreetmap.josm.tools.PlatformVisitor;
import org.openstreetmap.josm.tools.Utils;

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
        // Check is the grid is installed in default PROJ.4 directories
        File grid = Platform.determinePlatform().accept(this).stream()
                .map(dir -> new File(dir, gridFileName))
                .filter(file -> file.exists() && file.isFile())
                .findFirst().orElse(null);
        // If not, search into PROJ_LIB directory
        if (grid == null) {
            String projLib = Utils.getSystemProperty("PROJ_LIB");
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
                return Files.newInputStream(grid.getAbsoluteFile().toPath());
            } catch (IOException | InvalidPathException ex) {
                Logging.warn("Unable to open NTV2 grid shift file: " + grid);
                Logging.debug(ex);
            }
        }
        return null;
    }

    private static List<File> visit(String prefSuffix, String... defaults) {
        return Config.getPref().getList("ntv2.proj4.grid.dir." + prefSuffix, Arrays.asList(defaults))
                               .stream().map(File::new).collect(Collectors.toList());
    }

    @Override
    public List<File> visitUnixoid() {
        return visit("unix", "/usr/local/share/proj", "/usr/share/proj");
    }

    @Override
    public List<File> visitWindows() {
        return visit("windows", "C:\\PROJ\\NAD");
    }

    @Override
    public List<File> visitOsx() {
        return Collections.emptyList();
    }
}
