// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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
     * Creqates a stream iterating the list in reversed order
     * @param list the list to iterate over
     * @param <T> the type of elements in the list
     * @return a stream iterating the list in reversed order
     * @since 15732
     */
    public static <T> Stream<T> reversedStream(List<T> list) {
        Objects.requireNonNull(list, "list");
        final int size = list.size();
        return IntStream.range(0, size).mapToObj(i -> list.get(size - i - 1));
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

    /**
     * Creates a new Collector that collects the items in an unmodifiable list
     * @param <T> the type of the input elements
     * @return a new Collector that collects the items in an unmodifiable list
     * @see Utils#toUnmodifiableList
     * @since xxx
     */
    public static <T> Collector<T, ?, List<T>> toUnmodifiableList() {
        // Java 10: use java.util.stream.Collectors.toUnmodifiableList
        return Collectors.collectingAndThen(Collectors.toList(), Utils::toUnmodifiableList);
    }
}
