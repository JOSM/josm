// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Future;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.notes.Note;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.layer.NoteLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.BoundingBoxDownloader;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.OsmServerLocationReader;
import org.openstreetmap.josm.io.OsmServerReader;
import org.openstreetmap.josm.io.OsmTransferException;
import org.xml.sax.SAXException;

/** Task for downloading notes */
public class DownloadNotesTask extends AbstractDownloadTask {

    private static final String PATTERN_API_URL = "https?://.*/api/0.6/notes.*";
    private static final String PATTERN_DUMP_FILE = "https?://.*/(.*\\.osn(.bz2)?)";

    private DownloadTask downloadTask;

    public Future<?> download(boolean newLayer, long id, ProgressMonitor progressMonitor) {
        final String url = OsmApi.getOsmApi().getBaseUrl() + "notes/" + id;
        downloadTask = new DownloadRawUrlTask(new OsmServerLocationReader(url), progressMonitor);
        return Main.worker.submit(downloadTask);
    }

    @Override
    public Future<?> download(boolean newLayer, Bounds downloadArea, ProgressMonitor progressMonitor) {
        downloadTask = new DownloadBoundingBoxTask(new BoundingBoxDownloader(downloadArea), progressMonitor);
        return Main.worker.submit(downloadTask);
    }

    @Override
    public Future<?> loadUrl(boolean newLayer, String url, ProgressMonitor progressMonitor) {
        if (url.endsWith(".bz2")) {
            downloadTask = new DownloadBzip2RawUrlTask(new OsmServerLocationReader(url), progressMonitor);
        } else {
            downloadTask = new DownloadRawUrlTask(new OsmServerLocationReader(url), progressMonitor);
        }
        return Main.worker.submit(downloadTask);
    }

    @Override
    public void cancel() {
        if (downloadTask != null) {
            downloadTask.cancel();
        }
    }

    @Override
    public String getConfirmationMessage(URL url) {
        return null;
    }

    @Override
    public String getTitle() {
        return tr("Download OSM Notes");
    }

    @Override
    public String[] getPatterns() {
        return new String[] {PATTERN_API_URL, PATTERN_DUMP_FILE};
    }

    @Override
    public boolean isSafeForRemotecontrolRequests() {
        return true;
    }

    abstract class DownloadTask extends PleaseWaitRunnable {
        protected OsmServerReader reader;
        protected List<Note> notesData;

        public DownloadTask(OsmServerReader reader, ProgressMonitor progressMonitor) {
            super(tr("Downloading Notes"), progressMonitor, false);
            this.reader = reader;
        }

        @Override
        protected void finish() {
            if (isCanceled() || isFailed()) {
                return;
            }

            if (notesData == null) {
                return;
            }
            if (Main.isDebugEnabled()) {
                Main.debug("Notes downloaded: " + notesData.size());
            }

            List<NoteLayer> noteLayers = null;
            if (Main.map != null) {
                noteLayers = Main.map.mapView.getLayersOfType(NoteLayer.class);
            }
            NoteLayer layer;
            if (noteLayers != null && noteLayers.size() > 0) {
                layer = noteLayers.get(0);
                layer.getNoteData().addNotes(notesData);
            } else {
                layer = new NoteLayer(notesData, tr("Notes"));
                Main.main.addLayer(layer);
            }
        }

        @Override
        protected void cancel() {
            setCanceled(true);
            if (reader != null) {
                reader.cancel();
            }
        }

        @Override
        public abstract void realRun() throws IOException, SAXException, OsmTransferException;
    }

    class DownloadBoundingBoxTask extends DownloadTask {

        public DownloadBoundingBoxTask(OsmServerReader reader, ProgressMonitor progressMonitor) {
            super(reader, progressMonitor);
        }

        @Override
        public void realRun() throws IOException, SAXException, OsmTransferException {
            if (isCanceled()) {
                return;
            }
            ProgressMonitor subMonitor = progressMonitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false);
            try {
                notesData = reader.parseNotes(null, null, subMonitor);
            } catch (Exception e) {
                if (isCanceled())
                    return;
                if (e instanceof OsmTransferException) {
                    rememberException(e);
                } else {
                    rememberException(new OsmTransferException(e));
                }
            }
        }
    }

    class DownloadRawUrlTask extends DownloadTask {

        public DownloadRawUrlTask(OsmServerReader reader, ProgressMonitor progressMonitor) {
            super(reader, progressMonitor);
        }

        @Override
        public void realRun() throws IOException, SAXException, OsmTransferException {
            if (isCanceled()) {
                return;
            }
            ProgressMonitor subMonitor = progressMonitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false);
            try {
                notesData = reader.parseRawNotes(subMonitor);
            } catch (Exception e) {
                if (isCanceled())
                    return;
                if (e instanceof OsmTransferException) {
                    rememberException(e);
                } else {
                    rememberException(new OsmTransferException(e));
                }
            }
        }
    }

    class DownloadBzip2RawUrlTask extends DownloadTask {

        public DownloadBzip2RawUrlTask(OsmServerReader reader, ProgressMonitor progressMonitor) {
            super(reader, progressMonitor);
        }

        @Override
        public void realRun() throws IOException, SAXException, OsmTransferException {
            if (isCanceled()) {
                return;
            }
            ProgressMonitor subMonitor = progressMonitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false);
            try {
                notesData = reader.parseRawNotesBzip2(subMonitor);
            } catch (Exception e) {
                if (isCanceled())
                    return;
                if (e instanceof OsmTransferException) {
                    rememberException(e);
                } else {
                    rememberException(new OsmTransferException(e));
                }
            }
        }
    }
}
