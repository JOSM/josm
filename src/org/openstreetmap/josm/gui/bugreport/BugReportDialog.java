// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.bugreport;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ExpertToggleAction;
import org.openstreetmap.josm.gui.preferences.plugin.PluginPreference;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.gui.widgets.UrlLabel;
import org.openstreetmap.josm.plugins.PluginDownloadTask;
import org.openstreetmap.josm.plugins.PluginHandler;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.InputMapUtils;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.bugreport.BugReport;
import org.openstreetmap.josm.tools.bugreport.BugReportQueue.SuppressionMode;
import org.openstreetmap.josm.tools.bugreport.BugReportSender;
import org.openstreetmap.josm.tools.bugreport.BugReportSender.BugReportSendingHandler;
import org.openstreetmap.josm.tools.bugreport.ReportedException;

/**
 * This is a dialog that can be used to display a bug report.
 * <p>
 * It displays the bug to the user and asks the user to submit a bug report.
 * @author Michael Zangl
 * @since 10649
 */
public class BugReportDialog extends JDialog {
    private static final int MAX_MESSAGE_SIZE = 500;
    // This is explicitly not an ExtendedDialog - we still want to be able to display bug reports if there are problems with preferences/..
    private final JPanel content = new JPanel(new GridBagLayout());
    private final BugReport report;
    private final DebugTextDisplay textPanel;
    private JCheckBox cbSuppressSingle;
    private JCheckBox cbSuppressAll;

    /**
     * Default bug report callback that opens the bug report form in user browser
     * and displays a dialog in case of error.
     * @since 12790
     */
    public static final BugReportSendingHandler bugReportSendingHandler = new BugReportSendingHandler() {
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
                errorPanel.add(new UrlLabel(Main.getJOSMWebsite() + "/newticket", 2), GBC.eop().insets(8, 0, 0, 0));
                errorPanel.add(new DebugTextDisplay(statusText));

                JOptionPane.showMessageDialog(Main.parent, errorPanel, tr("You have encountered a bug in JOSM"),
                        JOptionPane.ERROR_MESSAGE);
            });
        }
    };

    /**
     * Create a new dialog.
     * @param report The report to display the dialog for.
     */
    public BugReportDialog(BugReport report) {
        super(findParent(), tr("You have encountered a bug in JOSM"));
        this.report = report;
        textPanel = new DebugTextDisplay(report);
        setContentPane(content);

        addMessageSection();

        addUpToDateSection();
        // TODO: Notify user about plugin updates, then remove that notification that is displayed before this dialog is displayed.

        addCreateTicketSection();

        if (ExpertToggleAction.isExpert()) {
            addDebugTextSection();
        }

        addIgnoreButton();

        pack();
        setModal(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        InputMapUtils.addEscapeAction(getRootPane(), new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                closeDialog();
            }
        });
    }

    /**
     * The message informing the user what happened.
     */
    private void addMessageSection() {
        String message = tr(
                "An unexpected exception occurred.\n" + "This is always a coding error. If you are running the latest "
                        + "version of JOSM, please consider being kind and file a bug report.");
        Icon icon = UIManager.getIcon("OptionPane.errorIcon");

        JPanel panel = new JPanel(new GridBagLayout());

        panel.add(new JLabel(icon), GBC.std().insets(0, 0, 10, 0));
        JMultilineLabel messageLabel = new JMultilineLabel(message);
        messageLabel.setMaxWidth(MAX_MESSAGE_SIZE);
        panel.add(messageLabel, GBC.eol().fill());
        content.add(panel, GBC.eop().fill(GBC.HORIZONTAL).insets(20, 10, 10, 10));
    }

    private void addDebugTextSection() {
        JPanel panel = new JPanel(new GridBagLayout());
        addBorder(panel, tr("Debug information"));
        panel.add(textPanel, GBC.eop().fill());

        panel.add(new JLabel(tr("Manually report at:")+' '), GBC.std());
        panel.add(new UrlLabel(Main.getJOSMWebsite() + "/newticket"), GBC.std().fill(GBC.HORIZONTAL));
        JButton copy = new JButton("Copy to clipboard");
        copy.addActionListener(e -> textPanel.copyToClipboard());
        panel.add(copy, GBC.eol().anchor(GBC.EAST));
        content.add(panel, GBC.eop().fill());
    }

    private void addUpToDateSection() {
        JPanel panel = new JosmUpdatePanel();
        addBorder(panel, tr("Is JOSM up to date?"));
        content.add(panel, GBC.eop().fill(GBC.HORIZONTAL));
    }

    private void addCreateTicketSection() {
        JPanel panel = new JPanel(new GridBagLayout());
        addBorder(panel, tr("Send bug report"));

        JMultilineLabel helpText = new JMultilineLabel(
                tr("If you are running the latest version of JOSM and the plugins, "
                        + "please file a bug report in our bugtracker.\n"
                        + "There the error information should already be "
                        + "filled in for you. Please include information on how to reproduce "
                        + "the error and try to supply as much detail as possible."));
        helpText.setMaxWidth(MAX_MESSAGE_SIZE);
        panel.add(helpText, GBC.eop().fill(GridBagConstraints.HORIZONTAL));

        Component settings = GBC.glue(0, 0);
        if (ExpertToggleAction.isExpert()) {
            // The default settings should be fine in most situations.
            settings = new BugReportSettingsPanel(report);
        }
        panel.add(settings);

        JButton sendBugReportButton = new JButton(tr("Report bug"), ImageProvider.getIfAvailable("bug"));
        sendBugReportButton.addActionListener(e -> sendBug());
        panel.add(sendBugReportButton, GBC.eol().insets(0, 0, 0, 0).anchor(GBC.SOUTHEAST));
        content.add(panel, GBC.eop().fill(GBC.HORIZONTAL));
    }

    private static void addBorder(JPanel panel, String title) {
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(title), BorderFactory
                .createEmptyBorder(5, 5, 5, 5)));
    }

    private void addIgnoreButton() {
        JPanel panel = new JPanel(new GridBagLayout());
        cbSuppressSingle = new JCheckBox(tr("Suppress this error for this session."));
        cbSuppressSingle.setVisible(false);
        panel.add(cbSuppressSingle, GBC.std(0, 0).fill(GBC.HORIZONTAL));
        cbSuppressAll = new JCheckBox(tr("Suppress further error dialogs for this session."));
        cbSuppressAll.setVisible(false);
        panel.add(cbSuppressAll, GBC.std(0, 1).fill(GBC.HORIZONTAL));
        JButton ignore = new JButton(tr("Ignore this error."));
        ignore.addActionListener(e -> closeDialog());
        panel.add(ignore, GBC.std(1, 0).span(1, 2).anchor(GBC.CENTER));
        content.add(panel, GBC.eol().fill(GBC.HORIZONTAL).insets(0, 0, 10, 10));
    }

    /**
     * Shows or hides the suppress errors button
     * @param showSuppress <code>true</code> to show the suppress errors checkbox.
     */
    public void setShowSuppress(boolean showSuppress) {
        cbSuppressSingle.setVisible(showSuppress);
        pack();
    }

    /**
     * Shows or hides the suppress all errors button
     * @param showSuppress <code>true</code> to show the suppress errors checkbox.
     * @since 10819
     */
    public void setShowSuppressAll(boolean showSuppress) {
        cbSuppressAll.setVisible(showSuppress);
        pack();
    }

    /**
     * Check if the checkbox to suppress further errors was selected
     * @return <code>true</code> if the user wishes to suppress errors.
     */
    public SuppressionMode shouldSuppressFurtherErrors() {
        if (cbSuppressAll.isSelected()) {
            return SuppressionMode.ALL;
        } else if (cbSuppressSingle.isSelected()) {
            return SuppressionMode.SAME;
        } else {
            return SuppressionMode.NONE;
        }
    }

    private void closeDialog() {
        setVisible(false);
    }

    private void sendBug() {
        BugReportSender.reportBug(textPanel.getCodeText());
    }

    /**
     * A safe way to find a matching parent frame.
     * @return The parent frame.
     */
    private static Frame findParent() {
        return (Frame) (Main.parent instanceof Frame ? Main.parent : SwingUtilities.getAncestorOfClass(Frame.class, Main.parent));
    }

    /**
     * Show the bug report for a given exception
     * @param e The exception to display
     * @param exceptionCounter A counter of how many exceptions have already been worked on
     * @return The new suppression status
     * @since 10819
     */
    public static SuppressionMode showFor(ReportedException e, int exceptionCounter) {
        if (e.isOutOfMemory()) {
            // do not translate the string, as translation may raise an exception
            JOptionPane.showMessageDialog(Main.parent, "JOSM is out of memory. " +
                    "Strange things may happen.\nPlease restart JOSM with the -Xmx###M option,\n" +
                    "where ### is the number of MB assigned to JOSM (e.g. 256).\n" +
                    "Currently, " + Runtime.getRuntime().maxMemory()/1024/1024 + " MB are available to JOSM.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                    );
            return SuppressionMode.NONE;
        } else {
            return GuiHelper.runInEDTAndWaitAndReturn(() -> {
                PluginDownloadTask downloadTask = PluginHandler.updateOrdisablePluginAfterException(e);
                if (downloadTask != null) {
                    // Ask for restart to install new plugin
                    PluginPreference.notifyDownloadResults(
                            Main.parent, downloadTask, !downloadTask.getDownloadedPlugins().isEmpty());
                    return SuppressionMode.NONE;
                }

                BugReport report = new BugReport(e);
                BugReportDialog dialog = new BugReportDialog(report);
                dialog.setShowSuppress(exceptionCounter > 0);
                dialog.setShowSuppressAll(exceptionCounter > 1);
                dialog.setVisible(true);
                return dialog.shouldSuppressFurtherErrors();
            });
        }
    }
}
