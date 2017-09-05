// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.io.IOException;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.UserIdentityManager;
import org.openstreetmap.josm.data.osm.UserInfo;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.ChangesetQuery;
import org.openstreetmap.josm.io.OsmServerUserInfoReader;
import org.openstreetmap.josm.io.OsmTransferCanceledException;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.ExceptionUtil;
import org.openstreetmap.josm.tools.Logging;
import org.xml.sax.SAXException;

/**
 * Asynchronous task to send a changeset query to the OSM API.
 * @since 2689
 */
public class ChangesetQueryTask extends AbstractChangesetDownloadTask {

    class DownloadTask extends RunnableDownloadTask {
        /** the changeset query */
        private ChangesetQuery query;
        /** the reader object used to read information about the current user from the API */
        private final OsmServerUserInfoReader userInfoReader = new OsmServerUserInfoReader();

        DownloadTask(Component parent, ChangesetQuery query) {
            super(parent, tr("Querying and downloading changesets"));
            this.query = query;
        }

        /**
         * Tries to fully identify the current JOSM user
         *
         * @throws OsmTransferException if something went wrong
         */
        protected void fullyIdentifyCurrentUser() throws OsmTransferException {
            getProgressMonitor().indeterminateSubTask(tr("Determine user id for current user..."));

            UserInfo info = userInfoReader.fetchUserInfo(getProgressMonitor().createSubTaskMonitor(1, false));
            UserIdentityManager im = UserIdentityManager.getInstance();
            im.setFullyIdentified(im.getUserName(), info);
        }

        @Override
        protected void realRun() throws SAXException, IOException, OsmTransferException {
            try {
                UserIdentityManager im = UserIdentityManager.getInstance();
                if (query.isRestrictedToPartiallyIdentifiedUser() && im.isCurrentUser(query.getUserName())) {
                    // if we query changesets for the current user, make sure we query against
                    // its user id, not its user name. If necessary, determine the user id first.
                    //
                    if (im.isPartiallyIdentified()) {
                        fullyIdentifyCurrentUser();
                    }
                    query = query.forUser(UserIdentityManager.getInstance().getUserId());
                }
                if (isCanceled())
                    return;
                getProgressMonitor().indeterminateSubTask(tr("Query and download changesets ..."));
                downloadedChangesets.addAll(reader.queryChangesets(query, getProgressMonitor().createSubTaskMonitor(0, false)));
            } catch (OsmTransferCanceledException e) {
                // thrown if user cancel the authentication dialog
                setCanceled(true);
                Logging.trace(e);
            } catch (OsmTransferException e) {
                if (isCanceled())
                    return;
                rememberLastException(e);
            }
        }

        @Override
        protected void finish() {
            rememberDownloadedData(downloadedChangesets);
            if (isCanceled())
                return;
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
            updateChangesets();
        }

        @Override
        protected void cancel() {
            super.cancel();
            synchronized (this) {
                if (userInfoReader != null) {
                    userInfoReader.cancel();
                }
            }
        }
    }

    /**
     * Creates the task.
     *
     * @param query the query to submit to the OSM server. Must not be null.
     * @throws IllegalArgumentException if query is null.
     */
    public ChangesetQueryTask(ChangesetQuery query) {
        this(Main.parent, query);
    }

    /**
     * Creates the task.
     *
     * @param parent the parent component relative to which the {@link org.openstreetmap.josm.gui.PleaseWaitDialog} is displayed.
     * Must not be null.
     * @param query the query to submit to the OSM server. Must not be null.
     * @throws IllegalArgumentException if query is null.
     * @throws IllegalArgumentException if parent is null
     */
    public ChangesetQueryTask(Component parent, ChangesetQuery query) {
        CheckParameterUtil.ensureParameterNotNull(query, "query");
        setDownloadTask(new DownloadTask(parent, query));
    }
}
