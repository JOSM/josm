// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.ChangesetCache;
import org.openstreetmap.josm.data.osm.UserInfo;
import org.openstreetmap.josm.gui.ExceptionDialogUtil;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.io.CloseChangesetDialog;
import org.openstreetmap.josm.gui.io.CloseChangesetTask;
import org.openstreetmap.josm.io.ChangesetQuery;
import org.openstreetmap.josm.io.ChangesetUpdater;
import org.openstreetmap.josm.io.NetworkManager;
import org.openstreetmap.josm.io.OnlineResource;
import org.openstreetmap.josm.io.OsmServerChangesetReader;
import org.openstreetmap.josm.io.OsmServerUserInfoReader;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.Shortcut;
import org.xml.sax.SAXException;

/**
 * User action to close open changesets.
 *
 * The list of open changesets will be downloaded from the server and presented
 * to the user.
 */
public class CloseChangesetAction extends JosmAction {

    /**
     * Constructs a new {@code CloseChangesetAction}.
     */
    public CloseChangesetAction() {
        super(tr("Close open changesets..."),
            "closechangeset",
            tr("Close open changesets"),
            Shortcut.registerShortcut("system:closechangeset",
                tr("File: {0}", tr("Close open changesets")),
                KeyEvent.VK_Q, Shortcut.ALT_CTRL),
            true
        );
        setHelpId(ht("/Action/CloseChangeset"));
        setEnabled(!NetworkManager.isOffline(OnlineResource.OSM_API));

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        MainApplication.worker.submit(new DownloadOpenChangesetsTask());
    }

    protected void onPostDownloadOpenChangesets() {
        ChangesetUpdater.check();
        List<Changeset> openChangesets = ChangesetCache.getInstance().getOpenChangesetsForCurrentUser();
        if (openChangesets.isEmpty()) {
            JOptionPane.showMessageDialog(
                    MainApplication.getMainFrame(),
                    tr("There are no open changesets"),
                    tr("No open changesets"),
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        CloseChangesetDialog dialog = new CloseChangesetDialog();
        dialog.setChangesets(openChangesets);
        dialog.setVisible(true);
        if (dialog.isCanceled())
            return;

        Collection<Changeset> changesetsToClose = dialog.getSelectedChangesets();
        CloseChangesetTask closeChangesetTask = new CloseChangesetTask(changesetsToClose);
        MainApplication.worker.submit(closeChangesetTask);
    }

    private final class DownloadOpenChangesetsTask extends PleaseWaitRunnable {

        private boolean canceled;
        private OsmServerChangesetReader reader;
        private List<Changeset> changesets;
        private Exception lastException;

        private DownloadOpenChangesetsTask() {
            super(tr("Downloading open changesets ..."), false /* don't ignore exceptions */);
        }

        @Override
        protected void cancel() {
            this.canceled = true;
            reader.cancel();
        }

        @Override
        protected void finish() {
            SwingUtilities.invokeLater(() -> {
                            if (lastException != null) {
                                ExceptionDialogUtil.explainException(lastException);
                            }
                            ChangesetCache.getInstance().update(changesets);
                            if (!canceled && lastException == null) {
                                onPostDownloadOpenChangesets();
                            }
                        });
        }

        /**
         * Fetch the user info from the server. This is necessary if we don't know the users id yet
         *
         * @return the user info
         * @throws OsmTransferException in case of any communication exception
         */
        private UserInfo fetchUserInfo() throws OsmTransferException {
            return new OsmServerUserInfoReader().fetchUserInfo(getProgressMonitor().createSubTaskMonitor(1, false));
        }

        @Override
        protected void realRun() throws SAXException, IOException, OsmTransferException {
            try {
                UserInfo userInfo = fetchUserInfo();
                if (canceled)
                    return;
                reader = new OsmServerChangesetReader();
                ChangesetQuery query = new ChangesetQuery().forUser(userInfo.getId()).beingOpen(true);
                changesets = reader.queryChangesets(
                        query,
                        getProgressMonitor().createSubTaskMonitor(1, false /* not internal */)
                );
            } catch (OsmTransferException | IllegalArgumentException e) {
                if (canceled)
                    return;
                lastException = e;
            }
        }

        /**
         * Determines if the download task has been canceled.
         * @return {@code true} if the download task has been canceled
         */
        public boolean isCanceled() {
            return canceled;
        }

        /**
         * Returns the last exception that occurred.
         * @return the last exception that occurred, or {@code null}
         */
        public Exception getLastException() {
            return lastException;
        }
    }
}
