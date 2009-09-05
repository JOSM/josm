// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.ExceptionDialogUtil;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.io.ChangesetProcessingType;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.Shortcut;
import org.xml.sax.SAXException;

import static org.openstreetmap.josm.tools.I18n.tr;

public class StopChangesetAction extends JosmAction{

    public StopChangesetAction() {
        super(tr("Close current changeset"),
                "closechangeset",
                tr("Close the current changeset ..."),
                Shortcut.registerShortcut(
                        "system:closechangeset",
                        tr("File: {0}", tr("Close the current changeset ...")),
                        KeyEvent.VK_Q,
                        Shortcut.GROUP_HOTKEY + Shortcut.GROUPS_ALT2
                ),
                true
        );

    }
    public void actionPerformed(ActionEvent e) {
        if (OsmApi.getOsmApi().getCurrentChangeset() == null) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("There is currently no changeset open."),
                    tr("No open changeset"),
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }
        Main.worker.submit(new StopChangesetActionTask());
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(Main.map != null && OsmApi.getOsmApi().getCurrentChangeset() != null);
    }

    static class StopChangesetActionTask extends PleaseWaitRunnable {
        private boolean cancelled;
        private Exception lastException;

        public StopChangesetActionTask() {
            super(tr("Closing changeset"), false /* don't ignore exceptions */);
        }
        @Override
        protected void cancel() {
            this.cancelled = true;
            OsmApi.getOsmApi().cancel();

        }

        @Override
        protected void finish() {
            if (cancelled)
                return;
            if (lastException != null) {
                ExceptionDialogUtil.explainException(lastException);
            }
        }

        @Override
        protected void realRun() throws SAXException, IOException, OsmTransferException {
            try {
                OsmApi.getOsmApi().stopChangeset(ChangesetProcessingType.USE_EXISTING_AND_CLOSE, getProgressMonitor().createSubTaskMonitor(1, false));
            } catch(Exception e) {
                if (cancelled)
                    return;
                lastException = e;
            }
        }
    }
}
