// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.ChangesetCache;
import org.openstreetmap.josm.data.osm.UserInfo;
import org.openstreetmap.josm.gui.ExceptionDialogUtil;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.io.ChangesetQuery;
import org.openstreetmap.josm.io.OsmServerChangesetReader;
import org.openstreetmap.josm.io.OsmServerUserInfoReader;
import org.openstreetmap.josm.io.OsmTransferException;
import org.xml.sax.SAXException;

/**
 * This is a task for downloading the open changesets of the current user
 * from the OSM server.
 *
 */
public class DownloadOpenChangesetsTask extends PleaseWaitRunnable {

    private boolean cancelled;
    private OsmServerChangesetReader reader;
    private List<Changeset> changesets;
    private OpenChangesetComboBoxModel model;
    private Exception lastException;
    private UserInfo userInfo;

    /**
     *
     * @param model provides the user id of the current user and accepts the changesets
     * after download
     */
    public DownloadOpenChangesetsTask(OpenChangesetComboBoxModel model) {
        super(tr("Downloading open changesets ...", false /* don't ignore exceptions */));
        this.model = model;
    }

    @Override
    protected void cancel() {
        this.cancelled = true;
        reader.cancel();
    }

    @Override
    protected void finish() {
        if (cancelled)
            return;
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
     * Fetch the user info from the server. This is necessary if we don't know
     * the users id yet
     *
     * @return the user info
     * @throws OsmTransferException thrown in case of any communication exception
     */
    protected UserInfo fetchUserInfo() throws OsmTransferException {
        OsmServerUserInfoReader reader = new OsmServerUserInfoReader();
        return reader.fetchUserInfo(getProgressMonitor().createSubTaskMonitor(1, false));
    }

    @Override
    protected void realRun() throws SAXException, IOException, OsmTransferException {
        try {
            if (model.getUserId()== 0) {
                userInfo = fetchUserInfo();
                model.setUserId(userInfo.getId());
            }
            if (cancelled)
                return;
            reader = new OsmServerChangesetReader();
            ChangesetQuery query = new ChangesetQuery().forUser((int)model.getUserId()).beingOpen(true);
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
