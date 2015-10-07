// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ShowStatusReportAction;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.preferences.plugin.PluginPreference;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.gui.widgets.JosmTextArea;
import org.openstreetmap.josm.gui.widgets.UrlLabel;
import org.openstreetmap.josm.plugins.PluginDownloadTask;
import org.openstreetmap.josm.plugins.PluginHandler;

/**
 * An exception handler that asks the user to send a bug report.
 *
 * @author imi
 */
public final class BugReportExceptionHandler implements Thread.UncaughtExceptionHandler {

    private static boolean handlingInProgress = false;
    private static volatile BugReporterThread bugReporterThread = null;
    private static int exceptionCounter = 0;
    private static boolean suppressExceptionDialogs = false;

    private static class BugReporterThread extends Thread {

        private final class BugReporterWorker implements Runnable {
            private final PluginDownloadTask pluginDownloadTask;

            private BugReporterWorker(PluginDownloadTask pluginDownloadTask) {
                this.pluginDownloadTask = pluginDownloadTask;
            }

            @Override
            public void run() {
                // Then ask for submitting a bug report, for exceptions thrown from a plugin too, unless updated to a new version
                if (pluginDownloadTask == null) {
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
                        } catch (IOException | NumberFormatException e) {
                            Main.warn("Unable to detect latest version of JOSM: "+e.getMessage());
                        }
                    }
                    // Show dialog
                    ExtendedDialog ed = new ExtendedDialog(Main.parent, tr("Unexpected Exception"), buttonTexts);
                    ed.setButtonIcons(buttonIcons);
                    ed.setIcon(JOptionPane.ERROR_MESSAGE);
                    ed.setCancelButton(1);
                    ed.setDefaultButton(defaultButtonIdx);
                    JPanel pnl = new JPanel(new GridBagLayout());
                    pnl.add(new JLabel("<html>" + message + "</html>"), GBC.eol());
                    JCheckBox cbSuppress = null;
                    if (exceptionCounter > 1) {
                        cbSuppress = new JCheckBox(tr("Suppress further error dialogs for this session."));
                        pnl.add(cbSuppress, GBC.eol());
                    }
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
                        } catch (IOException e) {
                            Main.warn("Unable to access JOSM website: "+e.getMessage());
                        }
                    } else {
                        // "Report bug"
                        askForBugReport(e);
                    }
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
        BugReporterThread(Throwable t) {
            super("Bug Reporter");
            this.e = t;
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
     * Handles the given throwable object
     * @param t The throwable object
     */
    public void handle(Throwable t) {
        handleException(t);
    }

    /**
     * Handles the given exception
     * @param e the exception
     */
    public static void handleException(final Throwable e) {
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

    private static void askForBugReport(final Throwable e) {
        try {
            final int maxlen = 6000;
            StringWriter stack = new StringWriter();
            e.printStackTrace(new PrintWriter(stack));

            String text = ShowStatusReportAction.getReportHeader() + stack.getBuffer().toString();
            String urltext = text.replaceAll("\r", "");
            if (urltext.length() > maxlen) {
                urltext = urltext.substring(0, maxlen);
                int idx = urltext.lastIndexOf('\n');
                // cut whole line when not loosing too much
                if (maxlen-idx < 200) {
                    urltext = urltext.substring(0, idx+1);
                }
                urltext += "...<snip>...\n";
            }

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
            p.add(getBugReportUrlLabel(urltext), GBC.eop().insets(8, 0, 0, 0));
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
            text = "{{{\n"+text+"}}}";

            if (Utils.copyToClipboard(text)) {
                p.add(new JLabel(tr("(The text has already been copied to your clipboard.)")),
                        GBC.eop().fill(GridBagConstraints.HORIZONTAL));
            }

            JosmTextArea info = new JosmTextArea(text, 18, 60);
            info.setCaretPosition(0);
            info.setEditable(false);
            p.add(new JScrollPane(info), GBC.eop().fill());

            for (Component c: p.getComponents()) {
                if (c instanceof JMultilineLabel) {
                    ((JMultilineLabel) c).setMaxWidth(400);
                }
            }

            JOptionPane.showMessageDialog(Main.parent, p, tr("You have encountered a bug in JOSM"), JOptionPane.ERROR_MESSAGE);
        } catch (Exception e1) {
            Main.error(e1);
        }
    }

    /**
     * Determines if an exception is currently being handled
     * @return {@code true} if an exception is currently being handled, {@code false} otherwise
     */
    public static boolean exceptionHandlingInProgress() {
        return handlingInProgress;
    }

    /**
     * Replies the URL to create a JOSM bug report with the given debug text
     * @param debugText The debug text to provide us
     * @return The URL to create a JOSM bug report with the given debug text
     * @since 5849
     */
    public static URL getBugReportUrl(String debugText) {
        try (
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            GZIPOutputStream gzip = new GZIPOutputStream(out)
        ) {
            gzip.write(debugText.getBytes(StandardCharsets.UTF_8));
            gzip.finish();

            return new URL(Main.getJOSMWebsite()+"/josmticket?" +
                    "gdata="+Base64.encode(ByteBuffer.wrap(out.toByteArray()), true));
        } catch (IOException e) {
            Main.error(e);
            return null;
        }
    }

    /**
     * Replies the URL label to create a JOSM bug report with the given debug text
     * @param debugText The debug text to provide us
     * @return The URL label to create a JOSM bug report with the given debug text
     * @since 5849
     */
    public static UrlLabel getBugReportUrlLabel(String debugText) {
        URL url = getBugReportUrl(debugText);
        if (url != null) {
            return new UrlLabel(url.toString(), Main.getJOSMWebsite()+"/josmticket?...", 2);
        }
        return null;
    }
}
