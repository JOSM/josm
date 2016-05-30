// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.bugreport;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.openstreetmap.josm.Main;

/**
 * This is a special exception that cannot be directly thrown.
 * <p>
 * It is used to capture more information about an exception that was already thrown.
 *
 * @author Michael Zangl
 * @see BugReport
 * @since 10285
 */
public class ReportedException extends RuntimeException {
    private static final int MAX_COLLECTION_ENTRIES = 30;
    /**
     *
     */
    private static final long serialVersionUID = 737333873766201033L;
    /**
     * We capture all stack traces on exception creation. This allows us to trace synchonization problems better. We cannot be really sure what
     * happened but we at least see which threads
     */
    private final transient Map<Thread, StackTraceElement[]> allStackTraces;
    private final transient LinkedList<Section> sections = new LinkedList<>();
    private final transient Thread caughtOnThread;
    private final Throwable exception;
    private String methodWarningFrom;

    ReportedException(Throwable exception) {
        this(exception, Thread.currentThread());
    }

    ReportedException(Throwable exception, Thread caughtOnThread) {
        super(exception);
        this.exception = exception;

        allStackTraces = Thread.getAllStackTraces();
        this.caughtOnThread = caughtOnThread;
    }

    /**
     * Displays a warning for this exception. The program can then continue normally. Does not block.
     */
    public void warn() {
        methodWarningFrom = BugReport.getCallingMethod(2);
        // TODO: Open the dialog.
    }

    /**
     * Starts a new debug data section. This normally does not need to be called manually.
     *
     * @param sectionName
     *            The section name.
     */
    public void startSection(String sectionName) {
        sections.add(new Section(sectionName));
    }

    /**
     * Prints the captured data of this report to a {@link PrintWriter}.
     *
     * @param out
     *            The writer to print to.
     */
    public void printReportDataTo(PrintWriter out) {
        out.println("=== REPORTED CRASH DATA ===");
        for (Section s : sections) {
            s.printSection(out);
            out.println();
        }

        if (methodWarningFrom != null) {
            out.println("Warning issued by: " + methodWarningFrom);
            out.println();
        }
    }

    /**
     * Prints the stack trace of this report to a {@link PrintWriter}.
     *
     * @param out
     *            The writer to print to.
     */
    public void printReportStackTo(PrintWriter out) {
        out.println("=== STACK TRACE ===");
        out.println(niceThreadName(caughtOnThread));
        getCause().printStackTrace(out);
        out.println();
    }

    /**
     * Prints the stack traces for other threads of this report to a {@link PrintWriter}.
     *
     * @param out
     *            The writer to print to.
     */
    public void printReportThreadsTo(PrintWriter out) {
        out.println("=== RUNNING THREADS ===");
        for (Entry<Thread, StackTraceElement[]> thread : allStackTraces.entrySet()) {
            out.println(niceThreadName(thread.getKey()));
            if (caughtOnThread.equals(thread.getKey())) {
                out.println("Stacktrace see above.");
            } else {
                for (StackTraceElement e : thread.getValue()) {
                    out.println(e);
                }
            }
            out.println();
        }
    }

    private static String niceThreadName(Thread thread) {
        String name = "Thread: " + thread.getName() + " (" + thread.getId() + ')';
        ThreadGroup threadGroup = thread.getThreadGroup();
        if (threadGroup != null) {
            name += " of " + threadGroup.getName();
        }
        return name;
    }

    /**
     * Checks if this exception is considered the same as an other exception. This is the case if both have the same cause and message.
     *
     * @param e
     *            The exception to check against.
     * @return <code>true</code> if they are considered the same.
     */
    public boolean isSame(ReportedException e) {
        if (!getMessage().equals(e.getMessage())) {
            return false;
        }

        Set<Throwable> dejaVu = Collections.newSetFromMap(new IdentityHashMap<Throwable, Boolean>());
        return hasSameStackTrace(dejaVu, this.exception, e.exception);
    }

    private static boolean hasSameStackTrace(Set<Throwable> dejaVu, Throwable e1, Throwable e2) {
        if (dejaVu.contains(e1)) {
            // cycle. If it was the same until here, we assume both have that cycle.
            return true;
        }
        dejaVu.add(e1);

        StackTraceElement[] t1 = e1.getStackTrace();
        StackTraceElement[] t2 = e2.getStackTrace();

        if (!Arrays.equals(t1, t2)) {
            return false;
        }

        Throwable c1 = e1.getCause();
        Throwable c2 = e2.getCause();
        if ((c1 == null) != (c2 == null)) {
            return false;
        } else if (c1 != null) {
            return hasSameStackTrace(dejaVu, c1, c2);
        } else {
            return true;
        }
    }

    /**
     * Adds some debug values to this exception.
     *
     * @param key
     *            The key to add this for. Does not need to be unique but it would be nice.
     * @param value
     *            The value.
     * @return This exception for easy chaining.
     */
    public ReportedException put(String key, Object value) {
        String string;
        try {
            if (value == null) {
                string = "null";
            } else if (value instanceof Collection) {
                string = makeCollectionNice((Collection<?>) value);
            } else if (value.getClass().isArray()) {
                string = makeCollectionNice(Arrays.asList(value));
            } else {
                string = value.toString();
            }
        } catch (RuntimeException t) {
            Main.warn(t);
            string = "<Error calling toString()>";
        }
        sections.getLast().put(key, string);
        return this;
    }

    private static String makeCollectionNice(Collection<?> value) {
        int lines = 0;
        StringBuilder str = new StringBuilder();
        for (Object e : value) {
            str.append("\n    - ");
            if (lines <= MAX_COLLECTION_ENTRIES) {
                str.append(e);
            } else {
                str.append("\n    ... (")
                   .append(value.size())
                   .append(" entries)");
                break;
            }
        }
        return str.toString();
    }

    @Override
    public String toString() {
        return new StringBuilder(48)
            .append("CrashReportedException [on thread ")
            .append(caughtOnThread)
            .append(']')
            .toString();
    }

    private static class SectionEntry {
        private final String key;
        private final String value;

        SectionEntry(String key, String value) {
            this.key = key;
            this.value = value;
        }

        /**
         * Prints this entry to the output stream in a line.
         * @param out The stream to print to.
         */
        public void print(PrintWriter out) {
            out.print(" - ");
            out.print(key);
            out.print(": ");
            out.println(value);
        }
    }

    private static class Section {

        private String sectionName;
        private ArrayList<SectionEntry> entries = new ArrayList<>();

        Section(String sectionName) {
            this.sectionName = sectionName;
        }

        /**
         * Add a key/value entry to this section.
         * @param key The key. Need not be unique.
         * @param value The value.
         */
        public void put(String key, String value) {
            entries.add(new SectionEntry(key, value));
        }

        /**
         * Prints this section to the output stream.
         * @param out The stream to print to.
         */
        public void printSection(PrintWriter out) {
            out.println(sectionName + ':');
            if (entries.isEmpty()) {
                out.println("No data collected.");
            } else {
                for (SectionEntry e : entries) {
                    e.print(out);
                }
            }
        }
    }
}
