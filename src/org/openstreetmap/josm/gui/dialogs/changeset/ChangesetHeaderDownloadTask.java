// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.ChangesetCache;
import org.openstreetmap.josm.gui.ExceptionDialogUtil;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.io.OsmServerChangesetReader;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.BugReportExceptionHandler;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.ExceptionUtil;
import org.xml.sax.SAXException;

/**
 * This is an asynchronous task for downloading a collection of changests from the OSM
 * server.
 *
 * The  task only downloads the changeset properties without the changeset content. It
 * updates the global {@link ChangesetCache}.
 *
 */
public class ChangesetHeaderDownloadTask extends PleaseWaitRunnable implements ChangesetDownloadTask{

    /**
     * Builds a download task from for a collection of changesets.
     *
     * Ignores null values and changesets with {@link Changeset#isNew()} == true.
     *
     * @param changesets the collection of changesets. Assumes an empty collection if null.
     * @return the download task
     */
    static public ChangesetHeaderDownloadTask buildTaskForChangesets(Collection<Changeset> changesets) {
        return buildTaskForChangesets(Main.parent, changesets);
    }

    /**
     * Builds a download task from for a collection of changesets.
     *
     * Ignores null values and changesets with {@link Changeset#isNew()} == true.
     *
     * @param parent the parent component relative to which the {@link org.openstreetmap.josm.gui.PleaseWaitDialog} is displayed.
     * Must not be null.
     * @param changesets the collection of changesets. Assumes an empty collection if null.
     * @return the download task
     * @throws IllegalArgumentException thrown if parent is null
     */
    static public ChangesetHeaderDownloadTask buildTaskForChangesets(Component parent, Collection<Changeset> changesets) {
        CheckParameterUtil.ensureParameterNotNull(parent, "parent");
        if (changesets == null) {
            changesets = Collections.emptyList();
        }

        HashSet<Integer> ids = new HashSet<Integer>();
        for (Changeset cs: changesets) {
            if (cs == null || cs.isNew()) {
                continue;
            }
            ids.add(cs.getId());
        }
        if (parent == null)
            return new ChangesetHeaderDownloadTask(ids);
        else
            return new ChangesetHeaderDownloadTask(parent, ids);

    }

    private Set<Integer> idsToDownload;
    private OsmServerChangesetReader reader;
    private boolean canceled;
    private Exception lastException;
    private Set<Changeset> downloadedChangesets;

    protected void init(Collection<Integer> ids) {
        if (ids == null) {
            ids = Collections.emptyList();
        }
        idsToDownload = new HashSet<Integer>();
        if (ids == null ||  ids.isEmpty())
            return;
        for (int id: ids) {
            if (id <= 0) {
                continue;
            }
            idsToDownload.add(id);
        }
    }

    /**
     * Creates the download task for a collection of changeset ids. Uses a {@link org.openstreetmap.josm.gui.PleaseWaitDialog}
     * whose parent is {@link Main#parent}.
     *
     * Null ids or or ids &lt;= 0 in the id collection are ignored.
     *
     * @param ids the collection of ids. Empty collection assumed if null.
     */
    public ChangesetHeaderDownloadTask(Collection<Integer> ids) {
        // parent for dialog is Main.parent
        super(tr("Download changesets"), false /* don't ignore exceptions */);
        init(ids);
    }

    /**
     * Creates the download task for a collection of changeset ids. Uses a {@link org.openstreetmap.josm.gui.PleaseWaitDialog}
     * whose parent is the parent window of <code>dialogParent</code>.
     *
     * Null ids or or ids &lt;= 0 in the id collection are ignored.
     *
     * @param dialogParent the parent reference component for the {@link org.openstreetmap.josm.gui.PleaseWaitDialog}. Must not be null.
     * @param ids the collection of ids. Empty collection assumed if null.
     * @throws IllegalArgumentException thrown if dialogParent is null
     */
    public ChangesetHeaderDownloadTask(Component dialogParent, Collection<Integer> ids) throws IllegalArgumentException{
        super(dialogParent,tr("Download changesets"), false /* don't ignore exceptions */);
        init(ids);
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
        if (canceled)
            return;
        if (lastException != null) {
            ExceptionDialogUtil.explainException(lastException);
        }
        Runnable r = new Runnable() {
            @Override
            public void run() {
                ChangesetCache.getInstance().update(downloadedChangesets);
            }
        };

        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(r);
            } catch(InterruptedException e) {
                Main.warn("InterruptedException in "+getClass().getSimpleName()+" while updating changeset cache");
            } catch(InvocationTargetException e) {
                Throwable t = e.getTargetException();
                if (t instanceof RuntimeException) {
                    BugReportExceptionHandler.handleException(t);
                } else if (t instanceof Exception){
                    ExceptionUtil.explainException(e);
                } else {
                    BugReportExceptionHandler.handleException(t);
                }
            }
        }
    }

    @Override
    protected void realRun() throws SAXException, IOException, OsmTransferException {
        try {
            synchronized (this) {
                reader = new OsmServerChangesetReader();
            }
            downloadedChangesets = new HashSet<Changeset>();
            downloadedChangesets.addAll(reader.readChangesets(idsToDownload, getProgressMonitor().createSubTaskMonitor(0, false)));
        } catch(OsmTransferException e) {
            if (canceled)
                // ignore exception if canceled
                return;
            // remember other exceptions
            lastException = e;
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
