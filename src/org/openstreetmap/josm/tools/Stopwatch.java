// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;

/**
 * Measures elapsed time in milliseconds
 *
 * @see <a href="https://guava.dev/releases/snapshot-jre/api/docs/com/google/common/base/Stopwatch.html">Stopwatch in Guava</a>
 * @since 15755
 */
public final class Stopwatch {
    private final long start;

    private Stopwatch(long start) {
        this.start = start;
    }

    /**
     * Creates and starts a stopwatch
     *
     * @return the started stopwatch
     */
    public static Stopwatch createStarted() {
        return new Stopwatch(System.currentTimeMillis());
    }

    /**
     * Returns the elapsed milliseconds
     *
     * @return the elapsed milliseconds
     */
    public long elapsed() {
        return System.currentTimeMillis() - start;
    }

    /**
     * Formats the duration since start as string
     *
     * @return the duration since start as string
     * @see Utils#getDurationString(long)
     */
    @Override
    public String toString() {
        // fix #11567 where elapsedTime is < 0
        return Utils.getDurationString(Math.max(0, elapsed()));
    }

    /**
     * Formats the given task name and duration since start as i18n string
     * @param taskName the task name
     * @return the task name and duration since start as i18n string
     */
    public String toString(String taskName) {
        return tr("{0} completed in {1}", taskName, toString());
    }
}
