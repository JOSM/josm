// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.bugreport;

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
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ExpertToggleAction;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.gui.widgets.UrlLabel;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.InputMapUtils;

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
    private JCheckBox cbSuppress;

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
        // TODO: Notify user about plugin updates

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
        content.add(panel, GBC.eop().fill(GBC.HORIZONTAL).insets(20));
    }

    private void addDebugTextSection() {
        JPanel panel = new JPanel(new GridBagLayout());
        addBorder(panel, tr("Debug information"));
        panel.add(textPanel, GBC.eop().fill());

        panel.add(new JLabel(tr("Manually report at:")), GBC.std());
        panel.add(new UrlLabel(Main.getJOSMWebsite() + "/newticket"), GBC.std().fill(GBC.HORIZONTAL));
        JButton copy = new JButton("Copy to clipboard");
        copy.addActionListener(e -> textPanel.copyToClippboard());
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

        if (ExpertToggleAction.isExpert()) {
            // The default settings should be fine in most situations.
            panel.add(new BugReportSettingsPanel(report), GBC.eop().fill(GBC.HORIZONTAL));
        }

        JButton sendBugReportButton = new JButton(tr("Report Bug"), ImageProvider.get("bug"));
        sendBugReportButton.addActionListener(e -> sendBug());
        panel.add(sendBugReportButton, GBC.eop().anchor(GBC.EAST));
        content.add(panel, GBC.eop().fill(GBC.HORIZONTAL));
    }

    private static void addBorder(JPanel panel, String title) {
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(title), BorderFactory
                .createEmptyBorder(10, 10, 10, 10)));
    }

    private void addIgnoreButton() {
        JPanel panel = new JPanel(new GridBagLayout());
        cbSuppress = new JCheckBox(tr("Suppress further error dialogs for this session."));
        cbSuppress.setVisible(false);
        panel.add(cbSuppress, GBC.std().fill(GBC.HORIZONTAL));
        JButton ignore = new JButton(tr("Ignore this error."));
        ignore.addActionListener(e -> closeDialog());
        panel.add(ignore, GBC.eol());
        content.add(panel, GBC.eol().fill(GBC.HORIZONTAL).insets(20));
    }

    /**
     * Shows or hides the suppress errors button
     * @param showSuppress <code>true</code> to show the suppress errors checkbox.
     */
    public void setShowSuppress(boolean showSuppress) {
        cbSuppress.setVisible(showSuppress);
        pack();
    }

    /**
     * Check if the checkbox to suppress further errors was selected
     * @return <code>true</code> if the user wishes to suppress errors.
     */
    public boolean shouldSuppressFurtherErrors() {
        return cbSuppress.isSelected();
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
        Component current = Main.parent;
        try {
            // avoid cycles/invalid hirarchies
            for (int i = 0; i < 20 && current != null; i++) {
                if (current instanceof Frame) {
                    return (Frame) current;
                }
                current = current.getParent();
            }
        } catch (RuntimeException e) {
            BugReport.intercept(e).put("current", current).warn();
        }
        return null;
    }
}
