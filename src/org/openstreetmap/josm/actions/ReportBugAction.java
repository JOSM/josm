// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.tools.BugReportExceptionHandler;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Utils;

/**
 * Reports a ticket to JOSM bugtracker.
 * @since 7624
 */
public class ReportBugAction extends JosmAction {

    /**
     * Constructs a new {@code ReportBugAction}.
     */
    public ReportBugAction() {
        super(tr("Report bug"), "bug", tr("Report a ticket to JOSM bugtracker"),
                Shortcut.registerShortcut("reportbug", tr("Report a ticket to JOSM bugtracker"),
                        KeyEvent.CHAR_UNDEFINED, Shortcut.NONE), true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        reportBug();
    }

    /**
     * Reports a ticket to JOSM bugtracker.
     */
    public static void reportBug() {
        reportBug(ShowStatusReportAction.getReportHeader());
    }

    /**
     * Reports a ticket to JOSM bugtracker with given status report.
     * @param report Status report header containing technical, non-personal information
     */
    public static void reportBug(String report) {
        OpenBrowser.displayUrl(BugReportExceptionHandler.getBugReportUrl(
                Utils.strip(report)).toExternalForm());
    }
}
