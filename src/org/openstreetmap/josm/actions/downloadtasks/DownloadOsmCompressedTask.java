// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import java.util.concurrent.Future;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.OsmServerLocationReader;
import org.openstreetmap.josm.io.OsmTransferException;

/**
 * Task allowing to download compressed OSM files (gzip and bzip2)
 * @since 5317
 */
public class DownloadOsmCompressedTask extends DownloadOsmTask {

    static final String PATTERN_GZ =  "https?://.*/.*\\.osm.(gz|bz2?)";

    @Override
    public String[] getPatterns() {
        return new String[]{PATTERN_GZ};
    }

    @Override
    public String getTitle() {
        return tr("Download Compressed OSM");
    }
    
    /* (non-Javadoc)
     * @see org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask#download(boolean, org.openstreetmap.josm.data.Bounds, org.openstreetmap.josm.gui.progress.ProgressMonitor)
     */
    @Override
    public Future<?> download(boolean newLayer, Bounds downloadArea,
            ProgressMonitor progressMonitor) {
        return null;
    }
    
    /**
     * Loads a given URL
     * @param new_layer {@code true} if the data should be saved to a new layer
     * @param url The URL as String
     * @param progressMonitor progress monitor for user interaction
     */
    @Override
    public Future<?> loadUrl(boolean new_layer, final String url, ProgressMonitor progressMonitor) {
        downloadTask = new DownloadTask(new_layer, new OsmServerLocationReader(url), progressMonitor) {
            @Override
            protected DataSet parseDataSet() throws OsmTransferException {
                ProgressMonitor subTaskMonitor = progressMonitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false);
                if (url.matches("https?://.*/.*\\.osm.bz2?")) {
                    return reader.parseOsmBzip2(subTaskMonitor);
                } else {
                    return reader.parseOsmGzip(subTaskMonitor);
                }
            }
        };
        currentBounds = null;
        // Extract .osm.gz/bz/bz2 filename from URL to set the new layer name
        extractOsmFilename("https?://.*/(.*\\.osm.(gz|bz2?))", url);
        return Main.worker.submit(downloadTask);
    }
}
