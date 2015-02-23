// License: GPL. For details, see Readme.txt file.
package org.openstreetmap.gui.jmapviewer;

import java.io.File;
import java.io.IOException;
import org.openstreetmap.gui.jmapviewer.interfaces.TileJob;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoaderListener;
import org.openstreetmap.gui.jmapviewer.interfaces.TileSource;

/**
 * Reworked version of the OsmFileCacheTileLoader.
 *
 * When class OsmFileCacheTileLoader is no longer needed, it can be integrated
 * here and removed.
 */
public class TMSFileCacheTileLoader extends OsmFileCacheTileLoader {

    public TMSFileCacheTileLoader(TileLoaderListener map, File cacheDir) throws IOException {
        super(map, cacheDir);
    }

    @Override
    public TileJob createTileLoaderJob(final Tile tile) {
        return new TMSFileLoadJob(tile);
    }

    protected class TMSFileLoadJob extends FileLoadJob {

        public TMSFileLoadJob(Tile tile) {
            super(tile);
        }

        @Override
        protected File getTileFile() {
            return getDataFile(tile.getSource().getTileType());
        }

        @Override
        protected File getTagsFile() {
            return getDataFile(TAGS_FILE_EXT);
        }

        protected File getDataFile(String ext) {
            int nDigits = (int) Math.ceil(Math.log10(1 << tile.getZoom()));
            String x = String.format("%0" + nDigits + "d", tile.getXtile());
            String y = String.format("%0" + nDigits + "d", tile.getYtile());
            File path = new File(tileCacheDir, "z" + tile.getZoom());
            for (int i=0; i<nDigits; i++) {
                String component = "x" + x.substring(i, i+1) + "y" + y.substring(i, i+1);
                if (i == nDigits -1 ) {
                    component += "." + ext;
                }
                path = new File(path, component);
            }
            return path;
        }
    }

    @Override
    protected File getSourceCacheDir(TileSource source) {
        File dir = sourceCacheDirMap.get(source);
        if (dir == null) {
            String id = source.getId();
            if (id != null) {
                dir = new File(cacheDirBase, id);
            } else {
                dir = new File(cacheDirBase, source.getName().replaceAll("[\\\\/:*?\"<>|]", "_"));
            }
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }
        return dir;
    }

}
