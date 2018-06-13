// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.concurrent.Future;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.Compression;
import org.openstreetmap.josm.io.OsmServerLocationReader;
import org.openstreetmap.josm.io.OsmTransferException;

/**
 * Task allowing to download compressed OSM-Change files (gzip, xz and bzip2)
 * @since 5361
 */
public class DownloadOsmChangeCompressedTask extends DownloadOsmChangeTask {

    private static final String PATTERN_COMPRESS = "https?://.*/(.*\\.osc.(gz|xz|bz2?|zip))";

    @Override
    public String[] getPatterns() {
        return new String[]{PATTERN_COMPRESS};
    }

    @Override
    public String getTitle() {
        return tr("Download Compressed OSM Change");
    }

    /**
     * Loads a given URL
     * @param settings download settings
     * @param url The URL as String
     * @param progressMonitor progress monitor for user interaction
     */
    @Override
    public Future<?> loadUrl(DownloadParams settings, final String url, ProgressMonitor progressMonitor) {
        downloadTask = new DownloadTask(settings, new OsmServerLocationReader(url), progressMonitor) {
            @Override
            protected DataSet parseDataSet() throws OsmTransferException {
                ProgressMonitor subTaskMonitor = progressMonitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false);
                return reader.parseOsmChange(subTaskMonitor, Compression.byExtension(url));
            }
        };
        currentBounds = null;
        // Extract .osc.gz/xz/bz/bz2/zip filename from URL to set the new layer name
        extractOsmFilename(settings, PATTERN_COMPRESS, url);
        return MainApplication.worker.submit(downloadTask);
    }
}
