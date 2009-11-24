// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.gui.help.HelpUtil.ht;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.UserInfo;
import org.openstreetmap.josm.gui.ExceptionDialogUtil;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.io.CloseChangesetDialog;
import org.openstreetmap.josm.gui.io.CloseChangesetTask;
import org.openstreetmap.josm.io.ChangesetQuery;
import org.openstreetmap.josm.io.OsmServerChangesetReader;
import org.openstreetmap.josm.io.OsmServerUserInfoReader;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.Shortcut;
import org.xml.sax.SAXException;

public class CloseChangesetAction extends JosmAction{

    public CloseChangesetAction() {
        super(tr("Close open changesets"),
                "closechangeset",
                tr("Closes open changesets"),
                Shortcut.registerShortcut(
                        "system:closechangeset",
                        tr("File: {0}", tr("Closes open changesets")),
                        KeyEvent.VK_Q,
                        Shortcut.GROUP_HOTKEY + Shortcut.GROUPS_ALT2
                ),
                true
        );
        putValue("help", ht("/Action/CloseChangeset"));

    }
    public void actionPerformed(ActionEvent e) {
        Main.worker.submit(new DownloadOpenChangesetsTask());
    }

    protected void onPostDownloadOpenChangesets(DownloadOpenChangesetsTask task) {
        if (task.isCancelled() || task.getLastException() != null) return;

        List<Changeset> openChangesets = task.getChangesets();
        if (openChangesets.isEmpty()) {
            JOptionPane.showMessageDialog(
                    Main.parent,
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
        Main.worker.submit(closeChangesetTask);
    }

    private class DownloadOpenChangesetsTask extends PleaseWaitRunnable {

        private boolean cancelled;
        private OsmServerChangesetReader reader;
        private List<Changeset> changesets;
        private Exception lastException;
        private UserInfo userInfo;

        /**
         *
         * @param model provides the user id of the current user and accepts the changesets
         * after download
         */
        public DownloadOpenChangesetsTask() {
            super(tr("Downloading open changesets ...", false /* don't ignore exceptions */));
        }

        @Override
        protected void cancel() {
            this.cancelled = true;
            reader.cancel();
        }

        @Override
        protected void finish() {
            SwingUtilities.invokeLater(
                    new Runnable() {
                        public void run() {
                            if (lastException != null) {
                                ExceptionDialogUtil.explainException(lastException);
                            }
                            onPostDownloadOpenChangesets(DownloadOpenChangesetsTask.this);
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
                userInfo = fetchUserInfo();
                if (cancelled)
                    return;
                reader = new OsmServerChangesetReader();
                ChangesetQuery query = new ChangesetQuery().forUser(userInfo.getId()).beingOpen();
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

        public List<Changeset> getChangesets() {
            return changesets;
        }

        public Exception getLastException() {
            return lastException;
        }
    }
}
