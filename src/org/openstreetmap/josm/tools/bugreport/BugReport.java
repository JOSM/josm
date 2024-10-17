// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.bugreport;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

import org.openstreetmap.josm.tools.Pair;

/**
 * This class contains utility methods to create and handle a bug report.
 * <p>
 * It allows you to configure the format and request to send the bug report.
 * <p>
 * It also contains the main entry point for all components to use the bug report system: Call {@link #intercept(Throwable)} to start handling an
 * exception.
 * <h1> Handling Exceptions </h1>
 * In your code, you should add try...catch blocks for any runtime exceptions that might happen. It is fine to catch throwable there.
 * <p>
 * You should then add some debug information there. This can be the OSM ids that caused the error, information on the data you were working on
 * or other local variables. Make sure that no exceptions may occur while computing the values. It is best to send plain local variables to
 * put(...). If you need to do computations, put them into a lambda expression. Then simply throw the throwable you got from the bug report.
 * The global exception handler will do the rest.
 * <pre>
 * int id = ...;
 * String tag = "...";
 * try {
 *   ... your code ...
 * } catch (RuntimeException t) {
 *   throw BugReport.intercept(t).put("id", id).put("tag", () → x.getTag());
 * }
 * </pre>
 *
 * Instead of re-throwing, you can call {@link ReportedException#warn()}. This will display a warning to the user and allow it to either report
 * the exception or ignore it.
 *
 * @author Michael Zangl
 * @since 10285
 */
public final class BugReport implements Serializable {
    private static final long serialVersionUID = 1L;
    /** The maximum suppressed exceptions to keep to report */
    private static final byte MAXIMUM_SUPPRESSED_EXCEPTIONS = 4;
    /** The list of suppressed exceptions, Pair&lt;time reported, exception&gt; */
    private static final Deque<Pair<Instant, Throwable>> SUPPRESSED_EXCEPTIONS = new ArrayDeque<>(MAXIMUM_SUPPRESSED_EXCEPTIONS);

    private boolean includeStatusReport = true;
    private boolean includeData = true;
    private boolean includeAllStackTraces;
    private final ReportedException exception;
    private final CopyOnWriteArrayList<BugReportListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Create a new bug report
     * @param e The {@link ReportedException} to use. No more data should be added after creating the report.
     */
    public BugReport(ReportedException e) {
        this.exception = e;
        includeAllStackTraces = e.mayHaveConcurrentSource();
    }

    /**
     * Add a suppressed exception. Mostly useful for when a chain of exceptions causes an actual bug report.
     * This should only be used when an exception is raised in {@link org.openstreetmap.josm.gui.util.GuiHelper}
     * or {@link org.openstreetmap.josm.gui.progress.swing.ProgressMonitorExecutor} at this time.
     * {@link org.openstreetmap.josm.tools.Logging} may call this in the future, when logging a throwable.
     * @param t The throwable raised. If {@code null}, we add a new {@code NullPointerException} instead.
     * @since 18549
     */
    public static void addSuppressedException(Throwable t) {
        // Ensure we don't call pop in more than MAXIMUM_SUPPRESSED_EXCEPTIONS threads. This guard is
        // here just in case someone doesn't read the javadocs.
        synchronized (SUPPRESSED_EXCEPTIONS) {
            SUPPRESSED_EXCEPTIONS.add(new Pair<>(Instant.now(), t != null ? t : new NullPointerException()));
            // Ensure we aren't keeping exceptions forever
            while (SUPPRESSED_EXCEPTIONS.size() > MAXIMUM_SUPPRESSED_EXCEPTIONS) {
                SUPPRESSED_EXCEPTIONS.pop();
            }
        }
    }

    /**
     * Determines if this report should include a system status report
     * @return <code>true</code> to include it.
     * @since 10597
     */
    public boolean isIncludeStatusReport() {
        return includeStatusReport;
    }

    /**
     * Set if this report should include a system status report
     * @param includeStatusReport if the status report should be included
     * @since 10585
     */
    public void setIncludeStatusReport(boolean includeStatusReport) {
        this.includeStatusReport = includeStatusReport;
        fireChange();
    }

    /**
     * Determines if this report should include the data that was traced.
     * @return <code>true</code> to include it.
     * @since 10597
     */
    public boolean isIncludeData() {
        return includeData;
    }

    /**
     * Set if this report should include the data that was traced.
     * @param includeData if data should be included
     * @since 10585
     */
    public void setIncludeData(boolean includeData) {
        this.includeData = includeData;
        fireChange();
    }

    /**
     * Determines if this report should include the stack traces for all other threads.
     * @return <code>true</code> to include it.
     * @since 10597
     */
    public boolean isIncludeAllStackTraces() {
        return includeAllStackTraces;
    }

    /**
     * Sets if this report should include the stack traces for all other threads.
     * @param includeAllStackTraces if all stack traces should be included
     * @since 10585
     */
    public void setIncludeAllStackTraces(boolean includeAllStackTraces) {
        this.includeAllStackTraces = includeAllStackTraces;
        fireChange();
    }

    /**
     * Gets the full string that should be send as error report.
     * @param header header text for the error report
     * @return The string.
     * @since 10585
     */
    public String getReportText(String header) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter out = new PrintWriter(stringWriter);
        if (isIncludeStatusReport()) {
            try {
                out.println(header);
            } catch (RuntimeException e) { // NOPMD
                out.println("Could not generate status report: " + e.getMessage());
            }
        }
        if (isIncludeData()) {
            exception.printReportDataTo(out);
            // Exceptions thrown in threads *may* be automatically wrapped by the thread handler (ForkJoinPool, etc.)
            // We want to keep the data saved in the child exceptions, so we print that as well.
            Throwable cause = exception.getCause();
            while (cause != null) {
                if (cause instanceof ReportedException) {
                    ((ReportedException) cause).printReportDataTo(out);
                }
                cause = cause.getCause();
            }
        }
        exception.printReportStackTo(out);
        if (isIncludeAllStackTraces()) {
            exception.printReportThreadsTo(out);
        }
        synchronized (SUPPRESSED_EXCEPTIONS) {
            if (!SUPPRESSED_EXCEPTIONS.isEmpty()) {
                out.println("=== ADDITIONAL EXCEPTIONS ===");
                // Avoid multiple bug reports from reading from the deque at the same time.
                while (SUPPRESSED_EXCEPTIONS.peek() != null) {
                    Pair<Instant, Throwable> currentException = SUPPRESSED_EXCEPTIONS.pop();
                    out.println("==== Exception at " + currentException.a.toEpochMilli() + " ====");
                    currentException.b.printStackTrace(out);
                }
            }
        }
        return stringWriter.toString().replaceAll("\r", "");
    }

    /**
     * Add a new change listener.
     * @param listener The listener
     * @since 10585
     */
    public void addChangeListener(BugReportListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove a change listener.
     * @param listener The listener
     * @since 10585
     */
    public void removeChangeListener(BugReportListener listener) {
        listeners.remove(listener);
    }

    private void fireChange() {
        listeners.forEach(l -> l.bugReportChanged(this));
    }

    /**
     * This should be called whenever you want to add more information to a given exception.
     * @param t The throwable that was thrown.
     * @return A {@link ReportedException} to which you can add additional information.
     */
    public static ReportedException intercept(Throwable t) {
        ReportedException e;
        if (t instanceof ReportedException) {
            e = (ReportedException) t;
        } else {
            e = new ReportedException(t);
        }
        e.startSection(getCallingMethod(2));
        return e;
    }

    /**
     * Find the method that called us.
     *
     * @param offset
     *            How many methods to look back in the stack trace. 1 gives the method calling this method, 0 gives you getCallingMethod().
     * @return The method name.
     */
    public static String getCallingMethod(int offset) {
        StackTraceElement found = getCallingMethod(offset + 1, BugReport.class.getName(), "getCallingMethod"::equals);
        if (found != null) {
            return found.getClassName().replaceFirst(".*\\.", "") + '#' + found.getMethodName();
        } else {
            return "?";
        }
    }

    /**
     * Find the method that called the given method on the current stack trace.
     * @param offset
     *           How many methods to look back in the stack trace.
     *           1 gives the method calling this method, 0 gives you the method with the given name..
     * @param className The name of the class to search for
     * @param methodName The name of the method to search for
     * @return The class and method name or null if it is unknown.
     */
    public static StackTraceElement getCallingMethod(int offset, String className, Predicate<String> methodName) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (int i = 0; i < stackTrace.length - offset; i++) {
            StackTraceElement element = stackTrace[i];
            if (className.equals(element.getClassName()) && methodName.test(element.getMethodName())) {
                return stackTrace[i + offset];
            }
        }
        return null;
    }

    /**
     * A listener that listens to changes to this report.
     * @author Michael Zangl
     * @since 10585
     */
    @FunctionalInterface
    public interface BugReportListener {
        /**
         * Called whenever this bug report was changed, e.g. the data to be included in it.
         * @param report The report that was changed.
         */
        void bugReportChanged(BugReport report);
    }
}
