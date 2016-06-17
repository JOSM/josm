// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.bugreport;

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
 * or other local variables. Make sure that no excpetions may occur while computing the values. It is best to send plain local variables to
 * put(...). Then simply throw the throwable you got from the bug report. The global exception handler will do the rest.
 * <pre>
 * int id = ...;
 * String tag = "...";
 * try {
 *   ... your code ...
 * } catch (Throwable t) {
 *   throw BugReport.intercept(t).put("id", id).put("tag", tag);
 * }
 * </pre>
 *
 * Instead of re-throwing, you can call {@link ReportedException#warn()}. This will display a warning to the user and allow it to either report
 * the execption or ignore it.
 *
 * @author Michael Zangl
 * @since 10285
 */
public final class BugReport {
    /**
     * Create a new bug report
     * @param e The {@link ReportedException} to use. No more data should be added after creating the report.
     */
    private BugReport(ReportedException e) {
        // TODO: Use this class to create the bug report.
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
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        String className = BugReport.class.getName();
        for (int i = 0; i < stackTrace.length - offset; i++) {
            StackTraceElement element = stackTrace[i];
            if (className.equals(element.getClassName()) && "getCallingMethod".equals(element.getMethodName())) {
                StackTraceElement toReturn = stackTrace[i + offset];
                return toReturn.getClassName().replaceFirst(".*\\.", "") + '#' + toReturn.getMethodName();
            }
        }
        return "?";
    }
}
