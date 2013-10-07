// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset.query;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.ChangesetCache;
import org.openstreetmap.josm.data.osm.UserInfo;
import org.openstreetmap.josm.gui.JosmUserIdentityManager;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.dialogs.changeset.ChangesetDownloadTask;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.ChangesetQuery;
import org.openstreetmap.josm.io.OsmServerChangesetReader;
import org.openstreetmap.josm.io.OsmServerUserInfoReader;
import org.openstreetmap.josm.io.OsmTransferCanceledException;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.BugReportExceptionHandler;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.ExceptionUtil;
import org.xml.sax.SAXException;

/**
 * Asynchronous task to send a changeset query to the OSM API.
 *
 */
public class ChangesetQueryTask extends PleaseWaitRunnable implements ChangesetDownloadTask{

    /** the changeset query */
    private ChangesetQuery query;
    /** true if the task was canceled */
    private boolean canceled;
    /** the set of downloaded changesets */
    private Set<Changeset> downloadedChangesets;
    /** the last exception remembered, if any */
    private Exception lastException;
    /** the reader object used to read information about the current user from the API */
    private OsmServerUserInfoReader userInfoReader;
    /** the reader object used to submit the changeset query to the API */
    private OsmServerChangesetReader changesetReader;

    /**
     * Creates the task.
     *
     * @param query the query to submit to the OSM server. Must not be null.
     * @throws IllegalArgumentException thrown if query is null.
     */
    public ChangesetQueryTask(ChangesetQuery query) throws IllegalArgumentException {
        super(tr("Querying and downloading changesets",false /* don't ignore exceptions */));
        CheckParameterUtil.ensureParameterNotNull(query, "query");
        this.query = query;
    }

    /**
     * Creates the task.
     *
     * @param parent the parent component relative to which the {@link org.openstreetmap.josm.gui.PleaseWaitDialog} is displayed.
     * Must not be null.
     * @param query the query to submit to the OSM server. Must not be null.
     * @throws IllegalArgumentException thrown if query is null.
     * @throws IllegalArgumentException thrown if parent is null
     */
    public ChangesetQueryTask(Component parent, ChangesetQuery query) throws IllegalArgumentException {
        super(parent, tr("Querying and downloading changesets"), false /* don't ignore exceptions */);
        CheckParameterUtil.ensureParameterNotNull(query, "query");
        this.query = query;
    }

    @Override
    protected void cancel() {
        canceled = true;
        synchronized(this) {
            if (userInfoReader != null) {
                userInfoReader.cancel();
            }
        }
        synchronized(this) {
            if (changesetReader != null) {
                changesetReader.cancel();
            }
        }
    }

    @Override
    protected void finish() {
        if (canceled) return;
        if (lastException != null) {
            GuiHelper.runInEDTAndWait(new Runnable() {
                private final Component parent = progressMonitor != null ? progressMonitor.getWindowParent() : null;
                @Override
                public void run() {
                    JOptionPane.showMessageDialog(
                            parent != null ? parent : Main.parent,
                            ExceptionUtil.explainException(lastException),
                            tr("Errors during download"),
                            JOptionPane.ERROR_MESSAGE);
                }
            });
            return;
        }

        // update the global changeset cache with the downloaded changesets.
        // this will trigger change events which views are listening to. They
        // will update their views accordingly.
        //
        // Run on the EDT because UI updates are triggered.
        //
        Runnable r = new Runnable() {
            @Override public void run() {
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

    /**
     * Tries to fully identify the current JOSM user
     *
     * @throws OsmTransferException thrown if something went wrong
     */
    protected void fullyIdentifyCurrentUser() throws OsmTransferException {
        getProgressMonitor().indeterminateSubTask(tr("Determine user id for current user..."));

        synchronized(this) {
            userInfoReader = new OsmServerUserInfoReader();
        }
        UserInfo info = userInfoReader.fetchUserInfo(getProgressMonitor().createSubTaskMonitor(1,false));
        synchronized(this) {
            userInfoReader = null;
        }
        JosmUserIdentityManager im = JosmUserIdentityManager.getInstance();
        im.setFullyIdentified(im.getUserName(), info);
    }

    @Override
    protected void realRun() throws SAXException, IOException, OsmTransferException {
        try {
            JosmUserIdentityManager im = JosmUserIdentityManager.getInstance();
            if (query.isRestrictedToPartiallyIdentifiedUser() && im.isCurrentUser(query.getUserName())) {
                // if we query changesets for the current user, make sure we query against
                // its user id, not its user name. If necessary, determine the user id
                // first.
                //
                if (im.isPartiallyIdentified() ) {
                    fullyIdentifyCurrentUser();
                }
                query = query.forUser(JosmUserIdentityManager.getInstance().getUserId());
            }
            if (canceled) return;
            getProgressMonitor().indeterminateSubTask(tr("Query and download changesets ..."));
            synchronized(this) {
                changesetReader= new OsmServerChangesetReader();
            }
            downloadedChangesets = new HashSet<Changeset>();
            downloadedChangesets.addAll(changesetReader.queryChangesets(query, getProgressMonitor().createSubTaskMonitor(0, false)));
            synchronized (this) {
                changesetReader = null;
            }
        } catch(OsmTransferCanceledException e) {
            // thrown if user cancel the authentication dialog
            canceled = true;
        }  catch(OsmTransferException e) {
            if (canceled)
                return;
            this.lastException = e;
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
