// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.util.Map;
import java.util.function.Function;

/**
 * Utility class to parse various types from strings.
 *
 * @since 16181
 */
public class StringParser extends GenericParser<String> {

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

    /**
     * Creates an empty {@code StringParser}
     */
    public StringParser() {
        super();
    }

    /**
     * Creates a new {@code StringParser} by deeply copying {@code parser}
     *
     * @param parser the parser to copy
     */
    public StringParser(StringParser parser) {
        super(parser);
    }

    private StringParser(Map<Class<?>, Function<String, ?>> parsers) {
        super(parsers);
    }

    @Override
    public <T> StringParser registerParser(Class<T> type, Function<String, T> value) {
        super.registerParser(type, value);
        return this;
    }
}
