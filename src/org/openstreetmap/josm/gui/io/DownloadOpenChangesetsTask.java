// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.io.IOException;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.ChangesetCache;
import org.openstreetmap.josm.data.osm.UserInfo;
import org.openstreetmap.josm.gui.ExceptionDialogUtil;
import org.openstreetmap.josm.gui.JosmUserIdentityManager;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.io.ChangesetQuery;
import org.openstreetmap.josm.io.OsmServerChangesetReader;
import org.openstreetmap.josm.io.OsmServerUserInfoReader;
import org.openstreetmap.josm.io.OsmTransferException;
import org.xml.sax.SAXException;

/**
 * This is a task for downloading the open changesets of the current user
 * from the OSM server.
 */
public class DownloadOpenChangesetsTask extends PleaseWaitRunnable {

    private boolean cancelled;
    private OsmServerChangesetReader reader;
    private List<Changeset> changesets;
    private Exception lastException;
    private Component parent;

    /**
     *
     * @param model provides the user id of the current user and accepts the changesets
     * after download
     */
    public DownloadOpenChangesetsTask(Component parent) {
        super(parent, tr("Downloading open changesets ..."), false /* don't ignore exceptions */);
        this.parent = parent;
    }

    @Override
    protected void cancel() {
        this.cancelled = true;
        synchronized(this) {
            if (reader != null) {
                reader.cancel();
            }
        }
    }

    @Override
    protected void finish() {
        if (JosmUserIdentityManager.getInstance().isAnonymous()) {
            JOptionPane.showMessageDialog(
                    JOptionPane.getFrameForComponent(parent),
                    "<html>" + tr("Could not retrieve the list of your open changesets because<br>"
                            + "JOSM does not know your identity.<br>"
                            + "You have either chosen to work anonymously or you are not entitled<br>"
                            + "to know the identity of the user on whose behalf you are working.")
                            + "</html>",
                    tr("Missing user identity"),
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        if (cancelled)return;
        if (lastException != null) {
            ExceptionDialogUtil.explainException(lastException);
            return;
        }
        if (changesets.isEmpty()) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("There are no open changesets"),
                    tr("No open changesets"),
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }
        SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                        ChangesetCache.getInstance().update(changesets);
                    }
                }
        );
    }

    /**
     * Refreshes the user info from the server. This is necessary if we don't know
     * the users id yet.
     *
     */
    protected void refreshUserIdentity(){
        JosmUserIdentityManager im = null;
        try {
            im = JosmUserIdentityManager.getInstance();
            OsmServerUserInfoReader reader = new OsmServerUserInfoReader();
            UserInfo info = reader.fetchUserInfo(getProgressMonitor().createSubTaskMonitor(1, false));
            im.setFullyIdentified(info.getDisplayName(), info);
        } catch(OsmTransferException e) {
            // retrieving the user info can fail if the current user is not authorised to
            // retrieve it, i.e. if he is working with an OAuth Access Token which doesn't
            // have the respective privileges or if he didn't or he can't authenticate with
            // a username/password-pair.
            //
            // Downgrade your knowlege about its identity if we've assumed that he was fully
            // identified. Otherwise, if he is anonymous or partially identified, keep our level
            // of knowlege.
            //
            if (im.isFullyIdentified()) {
                im.setPartiallyIdentified(im.getUserName());
            }
            System.err.println(tr("Warning: Failed to retrieve user infos for the current JOSM user. Exception was: {0}", e.toString()));
        }
    }

    @Override
    protected void realRun() throws SAXException, IOException, OsmTransferException {
        try {
            JosmUserIdentityManager im = JosmUserIdentityManager.getInstance();
            if (im.isAnonymous()) {
                refreshUserIdentity();
            } else if (im.isFullyIdentified()){
                // do nothing
            } else if (im.isPartiallyIdentified()) {
                refreshUserIdentity();
            }
            if (cancelled)return;
            synchronized(this) {
                reader = new OsmServerChangesetReader();
            }
            ChangesetQuery query = new ChangesetQuery().beingOpen(true);
            if (im.isAnonymous())
                // we still don't know anything about the current user. Can't retrieve
                // its changesets
                return;
            else if (im.isFullyIdentified()) {
                query = query.forUser(im.getUserId());
            } else {
                // we only know the users name, not its id. Nevermind, try to read
                // its open changesets anyway.
                //
                query = query.forUser(im.getUserName());
            }
            changesets = reader.queryChangesets(
                    query,
                    getProgressMonitor().createSubTaskMonitor(1, false /* not internal */)
            );
        } catch(Exception e) {
            if (cancelled)
                return;
            lastException = e;
        }
    }

    public boolean isCancelled() {
        return cancelled;
    }
}
