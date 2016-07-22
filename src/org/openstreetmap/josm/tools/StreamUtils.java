// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Utility methods for streams.
 * @author Michael Zangl
 * @since 10585
 */
public final class StreamUtils {

    /**
     * Utility class
     */
    private StreamUtils() {}

    /**
     * Convert an iterator to a stream.
     * @param <T> The element type to iterate over
     * @param iterator The iterator
     * @return The stream of for that iterator.
     */
    public static <T> Stream<T> toStream(Iterator<? extends T> iterator) {
        Spliterator<T> spliterator = Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED);
        return StreamSupport.stream(spliterator, false);
    }
}
