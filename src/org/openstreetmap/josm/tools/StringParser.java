// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Utility class to parse various types from strings.
 *
 * @since 16181
 */
public class StringParser {

    /**
     * The default instance supports parsing {@link String}, {@link Character}, {@link Boolean},
     * {@link Byte}, {@link Short}, {@link Integer}, {@link Long}, {@link Float}, {@link Double} (in their primitive and boxed form).
     */
    public static final StringParser DEFAULT = new StringParser(Utils.toUnmodifiableMap(new StringParser()
            .registerParser(String.class, Function.identity())
            .registerParser(char.class, value -> value.charAt(0))
            .registerParser(Character.class, value -> value.charAt(0))
            .registerParser(boolean.class, Boolean::parseBoolean)
            .registerParser(Boolean.class, Boolean::parseBoolean)
            .registerParser(byte.class, Byte::parseByte)
            .registerParser(Byte.class, Byte::parseByte)
            .registerParser(short.class, Short::parseShort)
            .registerParser(Short.class, Short::parseShort)
            .registerParser(int.class, Integer::parseInt)
            .registerParser(Integer.class, Integer::parseInt)
            .registerParser(long.class, Long::parseLong)
            .registerParser(Long.class, Long::parseLong)
            .registerParser(float.class, Float::parseFloat)
            .registerParser(Float.class, Float::parseFloat)
            .registerParser(double.class, Double::parseDouble)
            .registerParser(Double.class, Double::parseDouble)
            .parsers));

    private final Map<Class<?>, Function<String, ?>> parsers;

    public StringParser() {
        this(new LinkedHashMap<>());
    }

    /**
     * Creates a new {@code StringParser} by deeply copying {@code parser}
     *
     * @param parser the parser to copy
     */
    public StringParser(StringParser parser) {
        this(new LinkedHashMap<>(parser.parsers));
    }

    private StringParser(Map<Class<?>, Function<String, ?>> parsers) {
        this.parsers = parsers;
    }

    public <T> StringParser registerParser(Class<T> type, Function<String, T> value) {
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
     * Parses the given {@code string} as {@code type} and returns the result
     *
     * @param type   the type class
     * @param string the string to parse
     * @param <T>    the type
     * @return the parsed value for {@code string} as type {@code type}
     * @throws UnsupportedOperationException if {@code type} is not {@linkplain #supports supported}
     * @throws UncheckedParseException       when the parsing fails
     */
    @SuppressWarnings("unchecked")
    public <T> T parse(Class<T> type, String string) {
        final Function<String, ?> parser = parsers.get(type);
        if (parser == null) {
            throw new UnsupportedOperationException(type + " is not supported");
        }
        try {
            return (T) parser.apply(string);
        } catch (RuntimeException ex) {
            throw new UncheckedParseException("Failed to parse [" + string + "] as " + type, ex);
        }
    }

    /**
     * Tries to parse the given {@code string} as {@code type} and returns the result.
     *
     * @param type   the type class
     * @param string the string to parse
     * @param <T>    the type
     * @return the parsed value for {@code string} as type {@code type},
     * or {@code Optional.empty()} (if parsing fails, or the type is not {@linkplain #supports supported})
     */
    public <T> Optional<?> tryParse(Class<?> type, String string) {
        try {
            return Optional.ofNullable(parse(type, string));
        } catch (RuntimeException ex) {
            Logging.trace(ex);
            return Optional.empty();
        }
    }
}
