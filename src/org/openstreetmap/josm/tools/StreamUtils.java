// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Utility methods for streams.
 * @author Michael Zangl
 * @since 10585
 */
public final class StreamUtils {

    private static final class HtmlListCollector implements Collector<String, StringBuilder, String> {
        @Override
        public Supplier<StringBuilder> supplier() {
            return StringBuilder::new;
        }

        @Override
        public BiConsumer<StringBuilder, String> accumulator() {
            return (sb, item) -> sb.append("<li>").append(item).append("</li>");
        }

        @Override
        public BinaryOperator<StringBuilder> combiner() {
            return StringBuilder::append;
        }

        @Override
        public Function<StringBuilder, String> finisher() {
            return sb -> "<ul>" + sb.toString() + "</ul>";
        }

        @Override
        public Set<Characteristics> characteristics() {
            return EnumSet.of(Characteristics.CONCURRENT);
        }
    }

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

    /**
     * Creates a new Collector that collects the items and returns them as HTML unordered list.
     * @return The collector.
     * @since 10638
     */
    public static Collector<String, ?, String> toHtmlList() {
        return new HtmlListCollector();
    }
}
