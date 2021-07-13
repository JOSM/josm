// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.date;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Objects;

import org.openstreetmap.josm.tools.Utils;

/**
 * A timespan defined by a start and end instant.
 */
public final class Interval {

    private final Instant start;
    private final Instant end;

    /**
     * Constructs a new {@code Interval}
     * @param start start instant
     * @param end end instant
     */
    public Interval(Instant start, Instant end) {
        this.start = start;
        this.end = end;
    }

    /**
     * Returns an ISO 8601 compatible string
     * @return an ISO 8601 compatible string
     */
    @Override
    public String toString() {
        return start + "/" + end;
    }

    /**
     * Formats the interval of the given track as a human readable string
     * @return The interval as a string
     */
    public String format() {
        String ts = "";
        DateTimeFormatter df = DateUtils.getDateFormatter(FormatStyle.SHORT);
        String earliestDate = df.format(getStart());
        String latestDate = df.format(getEnd());

        if (earliestDate.equals(latestDate)) {
            DateTimeFormatter tf = DateUtils.getTimeFormatter(FormatStyle.SHORT);
            ts += earliestDate + ' ';
            ts += tf.format(getStart()) + " \u2013 " + tf.format(getEnd());
        } else {
            DateTimeFormatter dtf = DateUtils.getDateTimeFormatter(FormatStyle.SHORT, FormatStyle.MEDIUM);
            ts += dtf.format(getStart()) + " \u2013 " + dtf.format(getEnd());
        }

        ts += String.format(" (%s)", Utils.getDurationString(getEnd().toEpochMilli() - getStart().toEpochMilli()));
        return ts;
    }

    /**
     * Returns start instant.
     * @return start instant
     */
    public Instant getStart() {
        return start;
    }

    /**
     * Returns end instant.
     * @return end instant
     */
    public Instant getEnd() {
        return end;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Interval)) return false;
        Interval interval = (Interval) o;
        return Objects.equals(start, interval.start) && Objects.equals(end, interval.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }
}
