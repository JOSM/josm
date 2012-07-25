// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions.downloadtasks;

import java.util.concurrent.Future;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.OsmServerLocationReader;
import org.openstreetmap.josm.io.OsmTransferException;

public class DownloadOsmChangeCompressedTask extends DownloadOsmChangeTask {
    
    /* (non-Javadoc)
     * @see org.openstreetmap.josm.actions.downloadtasks.DownloadTask#acceptsUrl(java.lang.String)
     */
    @Override
    public boolean acceptsUrl(String url) {
        return url != null && url.matches("http://.*/.*\\.osc.(gz|bz2?)"); // Remote .osc.gz / .osc.bz / .osc.bz2 files
    }
        
    /**
     * Loads a given URL
     * @param True if the data should be saved to a new layer
     * @param The URL as String
     */
    public Future<?> loadUrl(boolean new_layer, final String url, ProgressMonitor progressMonitor) {
        downloadTask = new DownloadTask(new_layer, new OsmServerLocationReader(url), progressMonitor) {
            @Override
            protected DataSet parseDataSet() throws OsmTransferException {
                ProgressMonitor subTaskMonitor = progressMonitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false);
                if (url.matches("http://.*/.*\\.osc.bz2?")) {
                    return reader.parseOsmChangeBzip2(subTaskMonitor);
                } else {
                    return reader.parseOsmChangeGzip(subTaskMonitor);
                }
            }
        };
        currentBounds = null;
        // Extract .osc.gz/bz/bz2 filename from URL to set the new layer name
        extractOsmFilename("http://.*/(.*\\.osc.(gz|bz2?))", url);
        return Main.worker.submit(downloadTask);
    }
}
