// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.bugreport;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.gui.widgets.UrlLabel;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.bugreport.BugReportSender.BugReportSendingHandler;

/**
 * Default bug report callback that opens the bug report form in user browser
 * and displays a dialog in case of error.
 * @since 14176
 */
public class DefaultBugReportSendingHandler implements BugReportSendingHandler {

    @Override
    public String sendingBugReport(String bugUrl, String statusText) {
        return OpenBrowser.displayUrl(bugUrl);
    }

    @Override
    public void failed(String errorMessage, String statusText) {
        SwingUtilities.invokeLater(() -> {
            JPanel errorPanel = new JPanel(new GridBagLayout());
            errorPanel.add(new JMultilineLabel(
                    tr("Opening the bug report failed. Please report manually using this website:")),
                    GBC.eol().fill(GridBagConstraints.HORIZONTAL));
            errorPanel.add(new UrlLabel(Config.getUrls().getJOSMWebsite() + "/newticket", 2), GBC.eop().insets(8, 0, 0, 0));
            errorPanel.add(new DebugTextDisplay(statusText));

            JOptionPane.showMessageDialog(MainApplication.getMainFrame(), errorPanel, tr("You have encountered a bug in JOSM"),
                    JOptionPane.ERROR_MESSAGE);
        });
    }
}
