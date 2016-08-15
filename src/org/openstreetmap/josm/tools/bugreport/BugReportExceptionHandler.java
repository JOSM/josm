// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.bugreport;

/**
 * An exception handler that asks the user to send a bug report.
 *
 * @author imi
 * @since 40
 */
public final class BugReportExceptionHandler implements Thread.UncaughtExceptionHandler {

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        handleException(e);
    }

    /**
     * Handles the given exception
     * @param e the exception
     */
    public static synchronized void handleException(final Throwable e) {
        BugReport.intercept(e).warn();
    }

    /**
     * Determines if an exception is currently being handled
     * @return {@code true} if an exception is currently being handled, {@code false} otherwise
     */
    public static boolean exceptionHandlingInProgress() {
        return BugReportQueue.getInstance().exceptionHandlingInProgress();
    }
}
