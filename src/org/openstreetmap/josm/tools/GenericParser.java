// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Utility class to parse various types from other values.
 *
 * @since 16184
 */
public class GenericParser<U> {

    protected final Map<Class<?>, Function<U, ?>> parsers;

    public GenericParser() {
        this(new LinkedHashMap<>());
    }

    /**
     * Creates a new {@code GenericParser} by deeply copying {@code parser}
     *
     * @param parser the parser to copy
     */
    public GenericParser(GenericParser<U> parser) {
        this(new LinkedHashMap<>(parser.parsers));
    }

    protected GenericParser(Map<Class<?>, Function<U, ?>> parsers) {
        this.parsers = parsers;
    }

    public <T> GenericParser<U> registerParser(Class<T> type, Function<U, T> value) {
        parsers.put(type, value);
        return this;
    }

    /**
     * Determines whether {@code type} can be {@linkplain #parse parsed}
     *
     * @param type the type
     * @return true if {@code type} can be parsed
     */
    public boolean supports(Class<?> type) {
        return parsers.containsKey(type);
    }

    /**
     * Parses the given {@code value} as {@code type} and returns the result
     *
     * @param type  the type class
     * @param value the value to parse
     * @param <T>   the type
     * @return the parsed value for {@code string} as type {@code type}
     * @throws UnsupportedOperationException if {@code type} is not {@linkplain #supports supported}
     * @throws UncheckedParseException       when the parsing fails
     */
    @SuppressWarnings("unchecked")
    public <T> T parse(Class<T> type, U value) {
        final Function<U, ?> parser = parsers.get(type);
        if (parser == null) {
            throw new UnsupportedOperationException(type + " is not supported");
        }
        try {
            return (T) parser.apply(value);
        } catch (RuntimeException ex) {
            throw new UncheckedParseException("Failed to parse [" + value + "] as " + type, ex);
        }
    }

    /**
     * Tries to parse the given {@code value} as {@code type} and returns the result.
     *
     * @param type  the type class
     * @param value the value to parse
     * @param <T>   the type
     * @return the parsed value for {@code value} as type {@code type},
     * or {@code Optional.empty()} (if parsing fails, or the type is not {@linkplain #supports supported})
     */
    public <T> Optional<?> tryParse(Class<?> type, U value) {
        try {
            return Optional.ofNullable(parse(type, value));
        } catch (RuntimeException ex) {
            Logging.trace(ex);
            return Optional.empty();
        }
    }
}
