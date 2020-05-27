// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.bugreport.BugReportSender;

/**
 * Reports a ticket to JOSM bugtracker.
 * @since 7624
 */
public class ReportBugAction extends JosmAction {

    private final String text;

    /**
     * Constructs a new {@code ReportBugAction} that reports the normal status report.
     */
    public ReportBugAction() {
        this(null);
    }

    /**
     * Constructs a new {@link ReportBugAction} for the given debug text.
     * @param text The text to send
     */
    public ReportBugAction(String text) {
        super(tr("Report bug"), "bug", tr("Report a ticket to JOSM bugtracker"),
                Shortcut.registerShortcut("reportbug", tr("Report a ticket to JOSM bugtracker"),
                        KeyEvent.CHAR_UNDEFINED, Shortcut.NONE), true, false);
        this.text = text;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        BugReportSender.reportBug(text == null ? ShowStatusReportAction.getReportHeader() : text);
    }
}
