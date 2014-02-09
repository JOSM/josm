// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.ChangesetCache;
import org.openstreetmap.josm.data.osm.ChangesetDataSet;
import org.openstreetmap.josm.gui.ExceptionDialogUtil;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.io.OsmServerChangesetReader;
import org.openstreetmap.josm.io.OsmTransferCanceledException;
import org.openstreetmap.josm.io.OsmTransferException;
import org.xml.sax.SAXException;

/**
 * This is an asynchronous task for downloading the changeset content of a collection of
 * changesets.
 *
 */
public class ChangesetContentDownloadTask extends PleaseWaitRunnable implements ChangesetDownloadTask{

    /** the list of changeset ids to download */
    private final List<Integer> toDownload = new ArrayList<Integer>();
    /** true if the task was canceled */
    private boolean canceled;
    /** keeps the last exception thrown in the task, if any */
    private Exception lastException;
    /** the reader object used to read changesets from the API */
    private OsmServerChangesetReader reader;
    /** the set of downloaded changesets */
    private Set<Changeset> downloadedChangesets;

    /**
     * Initialize the task with a collection of changeset ids to download
     *
     * @param ids the collection of ids. May be null.
     */
    protected void init(Collection<Integer> ids) {
        if (ids == null) {
            ids = Collections.emptyList();
        }
        for (Integer id: ids) {
            if (id == null || id <= 0) {
                continue;
            }
            toDownload.add(id);
        }
        downloadedChangesets = new HashSet<Changeset>();
    }

    /**
     * Creates a download task for a single changeset
     *
     * @param changesetId the changeset id. &gt; 0 required.
     * @throws IllegalArgumentException thrown if changesetId &lt;= 0
     */
    public ChangesetContentDownloadTask(int changesetId) throws IllegalArgumentException{
        super(tr("Downloading changeset content"), false /* don't ignore exceptions */);
        if (changesetId <= 0)
            throw new IllegalArgumentException(MessageFormat.format("Expected integer value > 0 for parameter ''{0}'', got ''{1}''", "changesetId", changesetId));
        init(Collections.singleton(changesetId));
    }

    /**
     * Creates a download task for a collection of changesets. null values and id &lt;=0 in
     * the collection are sillently discarded.
     *
     * @param changesetIds the changeset ids. Empty collection assumed, if null.
     */
    public ChangesetContentDownloadTask(Collection<Integer> changesetIds) {
        super(tr("Downloading changeset content"), false /* don't ignore exceptions */);
        init(changesetIds);
    }

    /**
     * Creates a download task for a single changeset
     *
     * @param parent the parent component for the {@link org.openstreetmap.josm.gui.PleaseWaitDialog}. Must not be {@code null}.
     * @param changesetId the changeset id. {@code >0} required.
     * @throws IllegalArgumentException thrown if {@code changesetId <= 0}
     * @throws IllegalArgumentException thrown if parent is {@code null}
     */
    public ChangesetContentDownloadTask(Component parent, int changesetId) throws IllegalArgumentException{
        super(parent, tr("Downloading changeset content"), false /* don't ignore exceptions */);
        if (changesetId <= 0)
            throw new IllegalArgumentException(MessageFormat.format("Expected integer value > 0 for parameter ''{0}'', got ''{1}''", "changesetId", changesetId));
        init(Collections.singleton(changesetId));
    }

    /**
     * Creates a download task for a collection of changesets. null values and id &lt;=0 in
     * the collection are sillently discarded.
     *
     * @param parent the parent component for the {@link org.openstreetmap.josm.gui.PleaseWaitDialog}. Must not be {@code null}.
     * @param changesetIds the changeset ids. Empty collection assumed, if {@code null}.
     * @throws IllegalArgumentException thrown if parent is {@code null}
     */
    public ChangesetContentDownloadTask(Component parent, Collection<Integer> changesetIds) throws IllegalArgumentException {
        super(parent, tr("Downloading changeset content"), false /* don't ignore exceptions */);
        init(changesetIds);
    }

    /**
     * Replies true if the local {@link ChangesetCache} already includes the changeset with
     * id <code>changesetId</code>.
     *
     * @param changesetId the changeset id
     * @return true if the local {@link ChangesetCache} already includes the changeset with
     * id <code>changesetId</code>
     */
    protected boolean isAvailableLocally(int changesetId) {
        return ChangesetCache.getInstance().get(changesetId) != null;
    }

    /**
     * Downloads the changeset with id <code>changesetId</code> (only "header"
     * information, no content)
     *
     * @param changesetId the changeset id
     * @throws OsmTransferException thrown if something went wrong
     */
    protected void downloadChangeset(int changesetId) throws OsmTransferException {
        synchronized(this) {
            reader = new OsmServerChangesetReader();
        }
        Changeset cs = reader.readChangeset(changesetId, getProgressMonitor().createSubTaskMonitor(0, false));
        synchronized(this) {
            reader = null;
        }
        ChangesetCache.getInstance().update(cs);
    }

    @Override
    protected void cancel() {
        canceled = true;
        synchronized (this) {
            if (reader != null) {
                reader.cancel();
            }
        }
    }

    @Override
    protected void finish() {
        if (canceled) return;
        if (lastException != null) {
            ExceptionDialogUtil.explainException(lastException);
        }
    }

    @Override
    protected void realRun() throws SAXException, IOException, OsmTransferException {
        try {
            getProgressMonitor().setTicksCount(toDownload.size());
            int i=0;
            for (int id: toDownload) {
                i++;
                if (!isAvailableLocally(id)) {
                    getProgressMonitor().setCustomText(tr("({0}/{1}) Downloading changeset {2}...", i, toDownload.size(), id));
                    downloadChangeset(id);
                }
                if (canceled) return;
                synchronized(this) {
                    reader = new OsmServerChangesetReader();
                }
                getProgressMonitor().setCustomText(tr("({0}/{1}) Downloading content for changeset {2}...", i, toDownload.size(), id));
                ChangesetDataSet ds = reader.downloadChangeset(id, getProgressMonitor().createSubTaskMonitor(0, false));
                synchronized(this) {
                    reader = null;
                }
                Changeset cs = ChangesetCache.getInstance().get(id);
                cs.setContent(ds);
                ChangesetCache.getInstance().update(cs);
                downloadedChangesets.add(cs);
                getProgressMonitor().worked(1);
            }
        } catch(OsmTransferCanceledException e) {
            // the download was canceled by the user. This exception is caught if the
            // user canceled the authentication dialog.
            //
            canceled = true;
            return;
        } catch(OsmTransferException e) {
            if (canceled)
                return;
            lastException = e;
        } catch(RuntimeException e) {
            throw e;
        }
    }

    /* ------------------------------------------------------------------------------- */
    /* interface ChangesetDownloadTask                                                 */
    /* ------------------------------------------------------------------------------- */
    @Override
    public Set<Changeset> getDownloadedChangesets() {
        return downloadedChangesets;
    }

    @Override
    public boolean isCanceled() {
        return canceled;
    }

    @Override
    public boolean isFailed() {
        return lastException != null;
    }
}
