// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.concurrent.Future;

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

    private static final String PATTERN_COMPRESS =  "https?://.*/.*\\.osm.(gz|bz2?|zip)";

    @Override
    public String[] getPatterns() {
        return new String[]{PATTERN_COMPRESS};
    }

    @Override
    public String getTitle() {
        return tr("Download Compressed OSM");
    }

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
                } else if (url.matches("https?://.*/.*\\.osm.gz")) {
                    return reader.parseOsmGzip(subTaskMonitor);
                } else {
                    return reader.parseOsmZip(subTaskMonitor);
                }
            }
        };
        currentBounds = null;
        // Extract .osm.gz/bz/bz2/zip filename from URL to set the new layer name
        extractOsmFilename("https?://.*/(.*\\.osm.(gz|bz2?|zip))", url);
        return Main.worker.submit(downloadTask);
    }
}
