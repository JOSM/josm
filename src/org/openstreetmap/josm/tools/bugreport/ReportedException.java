// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.bugreport;

import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Supplier;

import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.StreamUtils;

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
    /**
     * How many entries of a collection to include in the bug report.
     */
    private static final int MAX_COLLECTION_ENTRIES = 30;

    private static final long serialVersionUID = 737333873766201033L;

    /**
     * We capture all stack traces on exception creation. This allows us to trace synchonization problems better.
     * We cannot be really sure what happened but we at least see which threads
     */
    private final transient Map<Thread, StackTraceElement[]> allStackTraces = new HashMap<>();
    private final LinkedList<Section> sections = new LinkedList<>();
    private final transient Thread caughtOnThread;
    private String methodWarningFrom;

    ReportedException(Throwable exception) {
        this(exception, Thread.currentThread());
    }

    ReportedException(Throwable exception, Thread caughtOnThread) {
        super(exception);

        try {
            allStackTraces.putAll(Thread.getAllStackTraces());
        } catch (SecurityException e) {
            Logging.log(Logging.LEVEL_ERROR, "Unable to get thread stack traces", e);
        }
        this.caughtOnThread = caughtOnThread;
    }

    /**
     * Displays a warning for this exception. The program can then continue normally. Does not block.
     */
    public void warn() {
        methodWarningFrom = BugReport.getCallingMethod(2);
        try {
            BugReportQueue.getInstance().submit(this);
        } catch (RuntimeException e) { // NOPMD
            Logging.error(e);
        }
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
        StringBuilder name = new StringBuilder("Thread: ").append(thread.getName()).append(" (").append(thread.getId()).append(')');
        ThreadGroup threadGroup = thread.getThreadGroup();
        if (threadGroup != null) {
            name.append(" of ").append(threadGroup.getName());
        }
        return name.toString();
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

        return hasSameStackTrace(new CauseTraceIterator(), e.getCause());
    }

    private static boolean hasSameStackTrace(CauseTraceIterator causeTraceIterator, Throwable e2) {
        if (!causeTraceIterator.hasNext()) {
            // all done.
            return true;
        }
        Throwable e1 = causeTraceIterator.next();
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
            return hasSameStackTrace(causeTraceIterator, c2);
        } else {
            return true;
        }
    }

    /**
     * Adds some debug values to this exception. The value is converted to a string. Errors during conversion are handled.
     *
     * @param key
     *            The key to add this for. Does not need to be unique but it would be nice.
     * @param value
     *            The value.
     * @return This exception for easy chaining.
     */
    public ReportedException put(String key, Object value) {
        return put(key, () -> value);
    }

    /**
    * Adds some debug values to this exception. This method automatically catches errors that occur during the production of the value.
    *
    * @param key
    *            The key to add this for. Does not need to be unique but it would be nice.
    * @param valueSupplier
    *            A supplier that is called once to get the value.
    * @return This exception for easy chaining.
    * @since 10586
    */
    public ReportedException put(String key, Supplier<Object> valueSupplier) {
        String string;
        try {
            Object value = valueSupplier.get();
            if (value == null) {
                string = "null";
            } else if (value instanceof Collection) {
                string = makeCollectionNice((Collection<?>) value);
            } else if (value.getClass().isArray()) {
                string = makeCollectionNice(Arrays.asList(value));
            } else {
                string = value.toString();
            }
        } catch (RuntimeException t) { // NOPMD
            Logging.warn(t);
            string = "<Error calling toString()>";
        }
        sections.getLast().put(key, string);
        return this;
    }

    private static String makeCollectionNice(Collection<?> value) {
        int lines = 0;
        StringBuilder str = new StringBuilder(32);
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
        return "ReportedException [thread=" + caughtOnThread + ", exception=" + getCause()
                + ", methodWarningFrom=" + methodWarningFrom + ']';
    }

    /**
     * Check if this exception may be caused by a threading issue.
     * @return <code>true</code> if it is.
     * @since 10585
     */
    public boolean mayHaveConcurrentSource() {
        return StreamUtils.toStream(CauseTraceIterator::new)
                .anyMatch(t -> t instanceof ConcurrentModificationException || t instanceof InvocationTargetException);
    }

    /**
     * Check if this is caused by an out of memory situaition
     * @return <code>true</code> if it is.
     * @since 10819
     */
    public boolean isOutOfMemory() {
        return StreamUtils.toStream(CauseTraceIterator::new).anyMatch(t -> t instanceof OutOfMemoryError);
    }

    /**
     * Iterates over the causes for this exception. Ignores cycles and aborts iteration then.
     * @author Michal Zangl
     * @since 10585
     */
    private final class CauseTraceIterator implements Iterator<Throwable> {
        private Throwable current = getCause();
        private final Set<Throwable> dejaVu = Collections.newSetFromMap(new IdentityHashMap<Throwable, Boolean>());

        @Override
        public boolean hasNext() {
            return current != null;
        }

        @Override
        public Throwable next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            Throwable toReturn = current;
            advance();
            return toReturn;
        }

        private void advance() {
            dejaVu.add(current);
            current = current.getCause();
            if (current != null && dejaVu.contains(current)) {
                current = null;
            }
        }
    }

    private static class SectionEntry implements Serializable {

        private static final long serialVersionUID = 1L;

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

    private static class Section implements Serializable {

        private static final long serialVersionUID = 1L;

        private final String sectionName;
        private final ArrayList<SectionEntry> entries = new ArrayList<>();

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
