// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.bugreport;

import java.awt.GraphicsEnvironment;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.preferences.plugin.PluginPreference;
import org.openstreetmap.josm.plugins.PluginDownloadTask;
import org.openstreetmap.josm.plugins.PluginHandler;

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
            if (GraphicsEnvironment.isHeadless()) {
                return;
            }
            BugReport report = new BugReport(BugReport.intercept(e));
            BugReportDialog dialog = new BugReportDialog(report);
            dialog.setShowSuppress(exceptionCounter > 1);
            dialog.setVisible(true);
            suppressExceptionDialogs = dialog.shouldSuppressFurtherErrors();
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

    /**
     * Determines if an exception is currently being handled
     * @return {@code true} if an exception is currently being handled, {@code false} otherwise
     */
    public static boolean exceptionHandlingInProgress() {
        return handlingInProgress;
    }
}
