// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.bugreport;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ReportBugAction;
import org.openstreetmap.josm.actions.ShowStatusReportAction;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.preferences.plugin.PluginPreference;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.gui.widgets.UrlLabel;
import org.openstreetmap.josm.plugins.PluginDownloadTask;
import org.openstreetmap.josm.plugins.PluginHandler;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.WikiReader;

/**
 * An exception handler that asks the user to send a bug report.
 *
 * @author imi
 * @since 40
 */
public final class BugReportExceptionHandler implements Thread.UncaughtExceptionHandler {

    private static boolean handlingInProgress;
    private static volatile BugReporterThread bugReporterThread;
    private static int exceptionCounter;
    private static boolean suppressExceptionDialogs;

    static final class BugReporterThread extends Thread {

        private final class BugReporterWorker implements Runnable {
            private final PluginDownloadTask pluginDownloadTask;

            private BugReporterWorker(PluginDownloadTask pluginDownloadTask) {
                this.pluginDownloadTask = pluginDownloadTask;
            }

            @Override
            public void run() {
                // Then ask for submitting a bug report, for exceptions thrown from a plugin too, unless updated to a new version
                if (pluginDownloadTask == null) {
                    askForBugReport(e);
                } else {
                    // Ask for restart to install new plugin
                    PluginPreference.notifyDownloadResults(
                            Main.parent, pluginDownloadTask, !pluginDownloadTask.getDownloadedPlugins().isEmpty());
                }
            }
        }

        private final Throwable e;

        /**
         * Constructs a new {@code BugReporterThread}.
         * @param t the exception
         */
        private BugReporterThread(Throwable t) {
            super("Bug Reporter");
            this.e = t;
        }

        static void askForBugReport(final Throwable e) {
            String[] buttonTexts = new String[] {tr("Do nothing"), tr("Report Bug")};
            String[] buttonIcons = new String[] {"cancel", "bug"};
            int defaultButtonIdx = 1;
            String message = tr("An unexpected exception occurred.<br>" +
                    "This is always a coding error. If you are running the latest<br>" +
                    "version of JOSM, please consider being kind and file a bug report."
                    );
            // Check user is running current tested version, the error may already be fixed
            int josmVersion = Version.getInstance().getVersion();
            if (josmVersion != Version.JOSM_UNKNOWN_VERSION) {
                try {
                    int latestVersion = Integer.parseInt(new WikiReader().
                            read(Main.getJOSMWebsite()+"/wiki/TestedVersion?format=txt").trim());
                    if (latestVersion > josmVersion) {
                        buttonTexts = new String[] {tr("Do nothing"), tr("Update JOSM"), tr("Report Bug")};
                        buttonIcons = new String[] {"cancel", "download", "bug"};
                        defaultButtonIdx = 2;
                        message = tr("An unexpected exception occurred. This is always a coding error.<br><br>" +
                                "However, you are running an old version of JOSM ({0}),<br>" +
                                "instead of using the current tested version (<b>{1}</b>).<br><br>"+
                                "<b>Please update JOSM</b> before considering to file a bug report.",
                                String.valueOf(josmVersion), String.valueOf(latestVersion));
                    }
                } catch (IOException | NumberFormatException ex) {
                    Main.warn("Unable to detect latest version of JOSM: "+ex.getMessage());
                }
            }
            // Build panel
            JPanel pnl = new JPanel(new GridBagLayout());
            pnl.add(new JLabel("<html>" + message + "</html>"), GBC.eol());
            JCheckBox cbSuppress = null;
            if (exceptionCounter > 1) {
                cbSuppress = new JCheckBox(tr("Suppress further error dialogs for this session."));
                pnl.add(cbSuppress, GBC.eol());
            }
            if (GraphicsEnvironment.isHeadless()) {
                return;
            }
            // Show dialog
            ExtendedDialog ed = new ExtendedDialog(Main.parent, tr("Unexpected Exception"), buttonTexts);
            ed.setButtonIcons(buttonIcons);
            ed.setIcon(JOptionPane.ERROR_MESSAGE);
            ed.setCancelButton(1);
            ed.setDefaultButton(defaultButtonIdx);
            ed.setContent(pnl);
            ed.setFocusOnDefaultButton(true);
            ed.showDialog();
            if (cbSuppress != null && cbSuppress.isSelected()) {
                suppressExceptionDialogs = true;
            }
            if (ed.getValue() <= 1) {
                // "Do nothing"
                return;
            } else if (ed.getValue() < buttonTexts.length) {
                // "Update JOSM"
                try {
                    Main.platform.openUrl(Main.getJOSMWebsite());
                } catch (IOException ex) {
                    Main.warn("Unable to access JOSM website: "+ex.getMessage());
                }
            } else {
                // "Report bug"
                try {
                    JPanel p = buildPanel(e);
                    JOptionPane.showMessageDialog(Main.parent, p, tr("You have encountered a bug in JOSM"), JOptionPane.ERROR_MESSAGE);
                } catch (RuntimeException ex) {
                    Main.error(ex);
                }
            }
        }

        @Override
        public void run() {
            // Give the user a chance to deactivate the plugin which threw the exception (if it was thrown from a plugin)
            SwingUtilities.invokeLater(new BugReporterWorker(PluginHandler.updateOrdisablePluginAfterException(e)));
        }
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        handleException(e);
    }

    /**
     * Handles the given exception
     * @param e the exception
     */
    public static synchronized void handleException(final Throwable e) {
        if (handlingInProgress || suppressExceptionDialogs)
            return;                  // we do not handle secondary exceptions, this gets too messy
        if (bugReporterThread != null && bugReporterThread.isAlive())
            return;
        handlingInProgress = true;
        exceptionCounter++;
        try {
            Main.error(e);
            if (Main.parent != null) {
                if (e instanceof OutOfMemoryError) {
                    // do not translate the string, as translation may raise an exception
                    JOptionPane.showMessageDialog(Main.parent, "JOSM is out of memory. " +
                            "Strange things may happen.\nPlease restart JOSM with the -Xmx###M option,\n" +
                            "where ### is the number of MB assigned to JOSM (e.g. 256).\n" +
                            "Currently, " + Runtime.getRuntime().maxMemory()/1024/1024 + " MB are available to JOSM.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                            );
                    return;
                }

                bugReporterThread = new BugReporterThread(e);
                bugReporterThread.start();
            }
        } finally {
            handlingInProgress = false;
        }
    }

    static JPanel buildPanel(final Throwable e) {
        StringWriter stack = new StringWriter();
        PrintWriter writer = new PrintWriter(stack);
        if (e instanceof ReportedException) {
            // Temporary!
            ((ReportedException) e).printReportDataTo(writer);
            ((ReportedException) e).printReportStackTo(writer);
        } else {
            e.printStackTrace(writer);
        }

        String text = ShowStatusReportAction.getReportHeader() + stack.getBuffer().toString();
        text = text.replaceAll("\r", "");

        JPanel p = new JPanel(new GridBagLayout());
        p.add(new JMultilineLabel(
                tr("You have encountered an error in JOSM. Before you file a bug report " +
                        "make sure you have updated to the latest version of JOSM here:")),
                        GBC.eol().fill(GridBagConstraints.HORIZONTAL));
        p.add(new UrlLabel(Main.getJOSMWebsite(), 2), GBC.eop().insets(8, 0, 0, 0));
        p.add(new JMultilineLabel(
                tr("You should also update your plugins. If neither of those help please " +
                        "file a bug report in our bugtracker using this link:")),
                        GBC.eol().fill(GridBagConstraints.HORIZONTAL));
        p.add(new JButton(new ReportBugAction(text)), GBC.eop().insets(8, 0, 0, 0));
        p.add(new JMultilineLabel(
                tr("There the error information provided below should already be " +
                        "filled in for you. Please include information on how to reproduce " +
                        "the error and try to supply as much detail as possible.")),
                        GBC.eop().fill(GridBagConstraints.HORIZONTAL));
        p.add(new JMultilineLabel(
                tr("Alternatively, if that does not work you can manually fill in the information " +
                        "below at this URL:")), GBC.eol().fill(GridBagConstraints.HORIZONTAL));
        p.add(new UrlLabel(Main.getJOSMWebsite()+"/newticket", 2), GBC.eop().insets(8, 0, 0, 0));

        // Wiki formatting for manual copy-paste
        DebugTextDisplay textarea = new DebugTextDisplay(text);

        if (textarea.copyToClippboard()) {
            p.add(new JLabel(tr("(The text has already been copied to your clipboard.)")),
                    GBC.eop().fill(GridBagConstraints.HORIZONTAL));
        }

        p.add(textarea, GBC.eop().fill());

        for (Component c: p.getComponents()) {
            if (c instanceof JMultilineLabel) {
                ((JMultilineLabel) c).setMaxWidth(400);
            }
        }
        return p;
    }

    /**
     * Determines if an exception is currently being handled
     * @return {@code true} if an exception is currently being handled, {@code false} otherwise
     */
    public static boolean exceptionHandlingInProgress() {
        return handlingInProgress;
    }
}
