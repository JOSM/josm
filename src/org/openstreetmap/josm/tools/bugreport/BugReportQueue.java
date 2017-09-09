// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.bugreport;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

import org.openstreetmap.josm.tools.Logging;

/**
 * This class handles the display of the bug report dialog.
 * @author Michael Zangl
 * @since 10819
 */
public class BugReportQueue {

    /**
     * The fallback bug report handler if none is set. Prints the stacktrace on standard error stream.
     * @since 12770
     */
    public static final BugReportHandler FALLBACK_BUGREPORT_HANDLER = (e, index) -> {
        e.printStackTrace();
        return BugReportQueue.SuppressionMode.NONE;
    };

    private static final BugReportQueue INSTANCE = new BugReportQueue();

    private final LinkedList<ReportedException> reportsToDisplay = new LinkedList<>();
    private boolean suppressAllMessages;
    private final ArrayList<ReportedException> suppressFor = new ArrayList<>();
    private Thread displayThread;
    private BugReportHandler bugReportHandler = FALLBACK_BUGREPORT_HANDLER;
    private final CopyOnWriteArrayList<Predicate<ReportedException>> handlers = new CopyOnWriteArrayList<>();
    private int displayedErrors;

    private boolean inReportDialog;

    /**
     * Class that handles reporting a bug to the user.
     */
    public interface BugReportHandler {
        /**
         * Handle the bug report for a given exception
         * @param e The exception to display
         * @param exceptionCounter A counter of how many exceptions have already been worked on
         * @return The new suppression status
         */
        SuppressionMode handle(ReportedException e, int exceptionCounter);
    }

    /**
     * The suppression mode that should be used after the dialog was closed.
     */
    public enum SuppressionMode {
        /**
         * Suppress no dialogs.
         */
        NONE,
        /**
         * Suppress only the ones that are for the same error
         */
        SAME,
        /**
         * Suppress all report dialogs
         */
        ALL
    }

    /**
     * Submit a new error to be displayed
     * @param report The error to display
     */
    public synchronized void submit(ReportedException report) {
        Logging.logWithStackTrace(Logging.LEVEL_ERROR, "Handled by bug report queue", report.getCause());
        if (suppressAllMessages || suppressFor.stream().anyMatch(report::isSame)) {
            Logging.info("User requested to skip error " + report);
        } else if (reportsToDisplay.size() > 100 || reportsToDisplay.stream().filter(report::isSame).count() >= 10) {
            Logging.warn("Too many errors. Dropping " + report);
        } else {
            reportsToDisplay.add(report);
            if (displayThread == null) {
                displayThread = new Thread(new BugReportDisplayRunnable(), "bug-report-display");
                displayThread.start();
            }
            notifyAll();
        }
    }

    private class BugReportDisplayRunnable implements Runnable {

        private volatile boolean running = true;

        @Override
        public void run() {
            try {
                while (running) {
                    ReportedException e = getNext();
                    handleDialogResult(e, displayFor(e));
                }
            } catch (InterruptedException e) {
                displayFor(BugReport.intercept(e));
                Thread.currentThread().interrupt();
            }
        }
    }

    private synchronized void handleDialogResult(ReportedException e, SuppressionMode suppress) {
        if (suppress == SuppressionMode.ALL) {
            suppressAllMessages = true;
            reportsToDisplay.clear();
        } else if (suppress == SuppressionMode.SAME) {
            suppressFor.add(e);
            reportsToDisplay.removeIf(e::isSame);
        }
        displayedErrors++;
        inReportDialog = false;
    }

    private synchronized ReportedException getNext() throws InterruptedException {
        while (reportsToDisplay.isEmpty()) {
            wait();
        }
        inReportDialog = true;
        return reportsToDisplay.removeFirst();
    }

    private SuppressionMode displayFor(ReportedException e) {
        if (handlers.stream().anyMatch(p -> p.test(e))) {
            Logging.trace("Intercepted by handler.");
            return SuppressionMode.NONE;
        }
        return bugReportHandler.handle(e, getDisplayedErrors());
    }

    private synchronized int getDisplayedErrors() {
        return displayedErrors;
    }

    /**
     * Check if the dialog is shown. Should only be used for e.g. debugging.
     * @return <code>true</code> if the exception handler is still showing the exception to the user.
     */
    public synchronized boolean exceptionHandlingInProgress() {
        return !reportsToDisplay.isEmpty() || inReportDialog;
    }

    /**
     * Sets the {@link BugReportHandler} for this queue.
     * @param bugReportHandler the handler in charge of displaying the bug report. Must not be null
     * @since 12770
     */
    public void setBugReportHandler(BugReportHandler bugReportHandler) {
        this.bugReportHandler = Objects.requireNonNull(bugReportHandler, "bugReportHandler");
    }

    /**
     * Allows you to peek or even intercept the bug reports.
     * @param handler The handler. It can return false to stop all further handling of the exception.
     * @since 10886
     */
    public void addBugReportHandler(Predicate<ReportedException> handler) {
        handlers.add(handler);
    }

    /**
     * Gets the global bug report queue
     * @return The queue
     * @since 10886
     */
    public static BugReportQueue getInstance() {
        return INSTANCE;
    }
}
