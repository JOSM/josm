// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.ChangesetCache;
import org.openstreetmap.josm.data.osm.ChangesetDataSet;
import org.openstreetmap.josm.gui.ExceptionDialogUtil;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.io.OsmTransferCanceledException;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.Logging;
import org.xml.sax.SAXException;

/**
 * This is an asynchronous task for downloading the changeset content of a collection of changesets.
 * @since 2689
 */
public class ChangesetContentDownloadTask extends AbstractChangesetDownloadTask {

    class DownloadTask extends RunnableDownloadTask {
        /** the list of changeset ids to download */
        private final List<Integer> toDownload = new ArrayList<>();

        DownloadTask(Component parent, Collection<Integer> ids) {
            super(parent, tr("Downloading changeset content"));
            for (Integer id: ids != null ? ids : Collections.<Integer>emptyList()) {
                if (id == null || id <= 0) {
                    continue;
                }
                toDownload.add(id);
            }
        }

        /**
         * Downloads the changeset with id <code>changesetId</code> (only "header" information, no content)
         *
         * @param changesetId the changeset id
         * @throws OsmTransferException if something went wrong
         */
        protected void downloadChangeset(int changesetId) throws OsmTransferException {
            Changeset cs = reader.readChangeset(changesetId, false, getProgressMonitor().createSubTaskMonitor(0, false));
            ChangesetCache.getInstance().update(cs);
        }

        @Override
        protected void realRun() throws SAXException, IOException, OsmTransferException {
            try {
                getProgressMonitor().setTicksCount(toDownload.size());
                int i = 0;
                for (int id: toDownload) {
                    i++;
                    if (!isAvailableLocally(id)) {
                        getProgressMonitor().setCustomText(tr("({0}/{1}) Downloading changeset {2}...", i, toDownload.size(), id));
                        downloadChangeset(id);
                    }
                    if (isCanceled())
                        return;
                    getProgressMonitor().setCustomText(tr("({0}/{1}) Downloading content for changeset {2}...", i, toDownload.size(), id));
                    ChangesetDataSet ds = reader.downloadChangeset(id, getProgressMonitor().createSubTaskMonitor(0, false));
                    Changeset cs = ChangesetCache.getInstance().get(id);
                    cs.setContent(ds);
                    ChangesetCache.getInstance().update(cs);
                    downloadedChangesets.add(cs);
                    getProgressMonitor().worked(1);
                }
            } catch (OsmTransferCanceledException e) {
                // the download was canceled by the user. This exception is caught if the user canceled the authentication dialog.
                setCanceled(true);
                Logging.trace(e);
            } catch (OsmTransferException e) {
                if (!isCanceled()) {
                    rememberLastException(e);
                }
            }
        }

        @Override
        protected void finish() {
            rememberDownloadedData(downloadedChangesets);
            if (isCanceled())
                return;
            if (lastException != null) {
                ExceptionDialogUtil.explainException(lastException);
            }
        }
    }

    /**
     * Creates a download task for a single changeset
     *
     * @param changesetId the changeset id. &gt; 0 required.
     * @throws IllegalArgumentException if changesetId &lt;= 0
     */
    public ChangesetContentDownloadTask(int changesetId) {
        this(MainApplication.getMainFrame(), changesetId);
    }

    /**
     * Creates a download task for a collection of changesets. null values and id &lt;=0 in
     * the collection are silently discarded.
     *
     * @param changesetIds the changeset ids. Empty collection assumed, if null.
     */
    public ChangesetContentDownloadTask(Collection<Integer> changesetIds) {
        this(MainApplication.getMainFrame(), changesetIds);
    }

    /**
     * Creates a download task for a single changeset
     *
     * @param parent the parent component for the {@link org.openstreetmap.josm.gui.PleaseWaitDialog}. Must not be {@code null}.
     * @param changesetId the changeset id. {@code >0} required.
     * @throws IllegalArgumentException if {@code changesetId <= 0}
     * @throws IllegalArgumentException if parent is {@code null}
     */
    public ChangesetContentDownloadTask(Component parent, int changesetId) {
        if (changesetId <= 0)
            throw new IllegalArgumentException(
                    MessageFormat.format("Expected integer value > 0 for parameter ''{0}'', got ''{1}''", "changesetId", changesetId));
        setDownloadTask(new DownloadTask(parent, Collections.singleton(changesetId)));
    }

    /**
     * Creates a download task for a collection of changesets. null values and id &lt;=0 in
     * the collection are sillently discarded.
     *
     * @param parent the parent component for the {@link org.openstreetmap.josm.gui.PleaseWaitDialog}. Must not be {@code null}.
     * @param changesetIds the changeset ids. Empty collection assumed, if {@code null}.
     * @throws IllegalArgumentException if parent is {@code null}
     */
    public ChangesetContentDownloadTask(Component parent, Collection<Integer> changesetIds) {
        setDownloadTask(new DownloadTask(parent, changesetIds));
    }

    /**
     * Replies true if the local {@link ChangesetCache} already includes the changeset with
     * id <code>changesetId</code>.
     *
     * @param changesetId the changeset id
     * @return true if the local {@link ChangesetCache} already includes the changeset with
     * id <code>changesetId</code>
     */
    protected static boolean isAvailableLocally(int changesetId) {
        return ChangesetCache.getInstance().get(changesetId) != null;
    }
}
