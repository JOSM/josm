package org.openstreetmap.josm.actions.downloadtasks;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.concurrent.Future;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.io.UpdatePrimitivesTask;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.OsmServerLocationReader;
import org.openstreetmap.josm.io.OsmServerReader;
import org.openstreetmap.josm.io.OsmTransferException;

public class DownloadOsmChangeTask extends DownloadOsmTask {

    @Override
    public boolean acceptsUrl(String url) {
        return url != null && url.matches("http://.*/api/0.6/changeset/\\p{Digit}+/download");
    }

    /* (non-Javadoc)
     * @see org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask#download(boolean, org.openstreetmap.josm.data.Bounds, org.openstreetmap.josm.gui.progress.ProgressMonitor)
     */
    @Override
    public Future<?> download(boolean newLayer, Bounds downloadArea,
            ProgressMonitor progressMonitor) {
        return null;
    }

    /* (non-Javadoc)
     * @see org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask#loadUrl(boolean, java.lang.String, org.openstreetmap.josm.gui.progress.ProgressMonitor)
     */
    @Override
    public Future<?> loadUrl(boolean new_layer, String url,
            ProgressMonitor progressMonitor) {
        downloadTask = new DownloadTask(new_layer,
                new OsmServerLocationReader(url),
                progressMonitor);
        return Main.worker.submit(downloadTask);
    }
    
    protected class DownloadTask extends DownloadOsmTask.DownloadTask {

        public DownloadTask(boolean newLayer, OsmServerReader reader,
                ProgressMonitor progressMonitor) {
            super(newLayer, reader, progressMonitor);
        }

        /* (non-Javadoc)
         * @see org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask.DownloadTask#parseDataSet()
         */
        @Override
        protected DataSet parseDataSet() throws OsmTransferException {
            return reader.parseOsmChange(progressMonitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false));
        }

        /* (non-Javadoc)
         * @see org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask.DownloadTask#finish()
         */
        @Override
        protected void finish() {
            super.finish();
            progressMonitor.subTask(tr("Updating data"));
            UpdatePrimitivesTask task = new UpdatePrimitivesTask(targetLayer, downloadedData.allPrimitives());
            Main.worker.submit(task);
        }
    }
}
