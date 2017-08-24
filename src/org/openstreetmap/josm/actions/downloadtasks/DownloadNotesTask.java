// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Future;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.ViewportData;
import org.openstreetmap.josm.data.notes.Note;
import org.openstreetmap.josm.data.osm.NoteData;
import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.layer.NoteLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.BoundingBoxDownloader;
import org.openstreetmap.josm.io.BoundingBoxDownloader.MoreNotesException;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.OsmServerLocationReader;
import org.openstreetmap.josm.io.OsmServerReader;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.Logging;
import org.xml.sax.SAXException;

/**
 * General task for downloading OSM notes.
 * <p>
 * It handles two URL patterns: API call and dump file export.
 * @since 7531
 */
public class DownloadNotesTask extends AbstractDownloadTask<NoteData> {

    private static final String PATTERN_API_URL = "https?://.*/api/0.6/notes.*";
    private static final String PATTERN_DUMP_FILE = "https?://.*/(.*\\.osn(.bz2)?)";
    /** Property defining the number of notes to be downloaded */
    public static final IntegerProperty DOWNLOAD_LIMIT = new IntegerProperty("osm.notes.downloadLimit", 1000);
    /** Property defining number of days a bug needs to be closed to no longer be downloaded */
    public static final IntegerProperty DAYS_CLOSED = new IntegerProperty("osm.notes.daysClosed", 7);

    private DownloadTask downloadTask;
    private NoteLayer noteLayer;

    /**
     * Download a specific note by its id.
     * @param id Note identifier
     * @param progressMonitor progress monitor
     * @return the future representing the asynchronous task
     */
    public Future<?> download(long id, ProgressMonitor progressMonitor) {
        final String url = OsmApi.getOsmApi().getBaseUrl() + "notes/" + id;
        downloadTask = new DownloadRawUrlTask(new OsmServerLocationReader(url), progressMonitor);
        return MainApplication.worker.submit(downloadTask);
    }

    @Override
    public Future<?> download(boolean newLayer, Bounds downloadArea, ProgressMonitor progressMonitor) {
        downloadTask = new DownloadBoundingBoxTask(new BoundingBoxDownloader(downloadArea), progressMonitor);
        return MainApplication.worker.submit(downloadTask);
    }

    @Override
    public Future<?> loadUrl(boolean newLayer, String url, ProgressMonitor progressMonitor) {
        if (url.endsWith(".bz2")) {
            downloadTask = new DownloadBzip2RawUrlTask(new OsmServerLocationReader(url), progressMonitor);
        } else {
            downloadTask = new DownloadRawUrlTask(new OsmServerLocationReader(url), progressMonitor);
        }
        return MainApplication.worker.submit(downloadTask);
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

    @Override
    public ProjectionBounds getDownloadProjectionBounds() {
        return noteLayer != null ? noteLayer.getViewProjectionBounds() : null;
    }

    abstract class DownloadTask extends PleaseWaitRunnable {
        protected OsmServerReader reader;
        protected List<Note> notesData;

        DownloadTask(OsmServerReader reader, ProgressMonitor progressMonitor) {
            super(tr("Downloading Notes"), progressMonitor, false);
            this.reader = reader;
        }

        @Override
        protected void finish() {
            rememberDownloadedData(new NoteData(notesData));
            if (isCanceled() || isFailed() || notesData == null || notesData.isEmpty()) {
                return;
            }
            if (Logging.isDebugEnabled()) {
                Logging.debug("Notes downloaded: {0}", notesData.size());
            }

            noteLayer = new NoteLayer(notesData, tr("Notes"));
            List<NoteLayer> noteLayers = Main.getLayerManager().getLayersOfType(NoteLayer.class);
            if (!noteLayers.isEmpty()) {
                noteLayers.get(0).getNoteData().addNotes(notesData);
                MapFrame map = MainApplication.getMap();
                if (map != null && zoomAfterDownload) {
                    map.mapView.scheduleZoomTo(new ViewportData(noteLayer.getViewProjectionBounds()));
                }
            } else {
                Main.getLayerManager().addLayer(noteLayer, zoomAfterDownload);
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

        DownloadBoundingBoxTask(OsmServerReader reader, ProgressMonitor progressMonitor) {
            super(reader, progressMonitor);
        }

        @Override
        public void realRun() throws IOException, SAXException, OsmTransferException {
            if (isCanceled()) {
                return;
            }
            ProgressMonitor subMonitor = progressMonitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false);
            try {
                notesData = reader.parseNotes(DOWNLOAD_LIMIT.get(), DAYS_CLOSED.get(), subMonitor);
            } catch (MoreNotesException e) {
                Logging.debug(e);
                notesData = e.notes;
                JOptionPane.showMessageDialog(Main.parent, "<html>"
                                + trn("{0} note has been downloaded.", "{0} notes have been downloaded.", e.limit, e.limit)
                                + "<br>"
                                + tr("Since the download limit was {0}, there might be more notes to download.", e.limit)
                                + "<br>"
                                + tr("Request a smaller area to make sure that all notes are being downloaded.")
                                + "</html>",
                        tr("More notes to download"), JOptionPane.INFORMATION_MESSAGE);
            } catch (OsmTransferException e) {
                if (isCanceled())
                    return;
                rememberException(e);
            }
        }
    }

    class DownloadRawUrlTask extends DownloadTask {

        DownloadRawUrlTask(OsmServerReader reader, ProgressMonitor progressMonitor) {
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
            } catch (OsmTransferException e) {
                if (isCanceled())
                    return;
                rememberException(e);
            }
        }
    }

    class DownloadBzip2RawUrlTask extends DownloadTask {

        DownloadBzip2RawUrlTask(OsmServerReader reader, ProgressMonitor progressMonitor) {
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
            } catch (OsmTransferException e) {
                if (isCanceled())
                    return;
                rememberException(e);
            }
        }
    }
}
