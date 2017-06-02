// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.util.StringJoiner;
import java.util.stream.Collector;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Utility methods for streams.
 * @author Michael Zangl
 */
public final class StreamUtils {

    /**
     * Utility class
     */
    private StreamUtils() {
        // Hide default constructor for utility classes
    }

    /**
     * Returns a sequential {@code Stream} with the iterable as its source.
     * @param <T> The element type to iterate over
     * @param iterable The iterable
     * @return The stream of for that iterable.
     * @since 10718
     */
    public static <T> Stream<T> toStream(Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    /**
     * Creates a new Collector that collects the items and returns them as HTML unordered list.
     * @return The collector.
     * @since 10638
     */
    public static Collector<String, ?, String> toHtmlList() {
        return Collector.of(
                () -> new StringJoiner("</li><li>", "<ul><li>", "</li></ul>").setEmptyValue("<ul></ul>"),
                StringJoiner::add, StringJoiner::merge, StringJoiner::toString
        );
    }
}
