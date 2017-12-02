// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils;

import static org.junit.Assert.fail;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import junit.framework.AssertionFailedError;

/**
 * Utilities to aid in making assertions about images using regular expressions.
 */
public final class ImagePatternMatching {
    private ImagePatternMatching() {}

    private static final Map<String, Pattern> patternCache = new HashMap<>();

    private static Matcher imageStripPatternMatchInner(
        final BufferedImage image,
        final int columnOrRowIndex,
        IntFunction<String> paletteMapFn,
        final Map<Integer, String> paletteMap,
        Pattern pattern,
        final String patternString,
        final boolean isColumn,
        final boolean assertMatch
    ) {
        paletteMapFn = Optional.ofNullable(paletteMapFn)
            // using "#" as the default "unmapped" character as it can be used in regexes without escaping
            .orElse(i -> paletteMap.getOrDefault(i, "#"));
        pattern = Optional.ofNullable(pattern)
            .orElseGet(() -> patternCache.computeIfAbsent(patternString, k -> Pattern.compile(k)));

        int[] columnOrRow = isColumn
            ? image.getRGB(columnOrRowIndex, 0, 1, image.getHeight(), null, 0, 1)
            : image.getRGB(0, columnOrRowIndex, image.getWidth(), 1, null, 0, image.getWidth());

        String stringRepr = Arrays.stream(columnOrRow).mapToObj(paletteMapFn).collect(Collectors.joining());
        Matcher result = pattern.matcher(stringRepr);

        if (assertMatch && !result.matches()) {
            System.err.println(String.format("Full strip failing to match pattern %s: %s", pattern, stringRepr));
            fail(String.format(
                "%s %d failed to match pattern %s",
                isColumn ? "Column" : "Row",
                columnOrRowIndex,
                pattern
            ));
        }

        return result;
    }

    /**
     * Attempt to match column {@code colNumber}, once translated to characters according to {@code paletteMap}
     * against the regular expression described by {@code patternString}.
     *
     * @param image         image to take column from
     * @param colNumber     image column number for comparison
     * @param paletteMap    {@link Map} of {@code Integer}s (denoting the color in ARGB format) to {@link String}s. It
     *                      is advised to only map colors to single characters. Colors with no corresponding entry in
     *                      the map are mapped to {@code #}.
     * @param patternString string representation of regular expression to match against. These are simply used to
     *                      construct a {@link Pattern} which is cached in case of re-use.
     * @param assertMatch   whether to raise an (informative) {@link AssertionFailedError} if no match is found.
     * @return {@link Matcher} produced by matching attempt
     */
    public static Matcher columnMatch(
        final BufferedImage image,
        final int colNumber,
        final Map<Integer, String> paletteMap,
        final String patternString,
        final boolean assertMatch
    ) {
        return imageStripPatternMatchInner(
            image,
            colNumber,
            null,
            paletteMap,
            null,
            patternString,
            true,
            true
        );
    }

    /**
     * Attempt to match column {@code colNumber}, once translated to characters according to {@code paletteMapFn}
     * against the regular expression described by {@code patternString}.
     *
     * @param image         image to take column from
     * @param colNumber     image column number for comparison
     * @param paletteMapFn  function mapping {@code Integer}s (denoting the color in ARGB format) to {@link String}s. It
     *                      is advised to only map colors to single characters.
     * @param patternString string representation of regular expression to match against. These are simply used to
     *                      construct a {@link Pattern} which is cached in case of re-use.
     * @param assertMatch   whether to raise an (informative) {@link AssertionFailedError} if no match is found.
     * @return {@link Matcher} produced by matching attempt
     */
    public static Matcher columnMatch(
        final BufferedImage image,
        final int colNumber,
        final IntFunction<String> paletteMapFn,
        final String patternString,
        final boolean assertMatch
    ) {
        return imageStripPatternMatchInner(
            image,
            colNumber,
            paletteMapFn,
            null,
            null,
            patternString,
            true,
            true
        );
    }

    /**
     * Attempt to match column {@code colNumber}, once translated to characters according to {@code paletteMap}
     * against the regular expression {@code pattern}.
     *
     * @param image         image to take column from
     * @param colNumber     image column number for comparison
     * @param paletteMap    {@link Map} of {@code Integer}s (denoting the color in ARGB format) to {@link String}s. It
     *                      is advised to only map colors to single characters. Colors with no corresponding entry in
     *                      the map are mapped to {@code #}.
     * @param pattern       regular expression to match against
     * @param assertMatch   whether to raise an (informative) {@link AssertionFailedError} if no match is found.
     * @return {@link Matcher} produced by matching attempt
     */
    public static Matcher columnMatch(
        final BufferedImage image,
        final int colNumber,
        final Map<Integer, String> paletteMap,
        final Pattern pattern,
        final boolean assertMatch
    ) {
        return imageStripPatternMatchInner(
            image,
            colNumber,
            null,
            paletteMap,
            pattern,
            null,
            true,
            true
        );
    }

    /**
     * Attempt to match column {@code colNumber}, once translated to characters according to {@code paletteMapFn}
     * against the regular expression {@code pattern}.
     *
     * @param image         image to take column from
     * @param colNumber     image column number for comparison
     * @param paletteMapFn  function mapping {@code Integer}s (denoting the color in ARGB format) to {@link String}s. It
     *                      is advised to only map colors to single characters.
     * @param pattern       regular expression to match against
     * @param assertMatch   whether to raise an (informative) {@link AssertionFailedError} if no match is found.
     * @return {@link Matcher} produced by matching attempt
     */
    public static Matcher columnMatch(
        final BufferedImage image,
        final int colNumber,
        final IntFunction<String> paletteMapFn,
        final Pattern pattern,
        final boolean assertMatch
    ) {
        return imageStripPatternMatchInner(
            image,
            colNumber,
            paletteMapFn,
            null,
            pattern,
            null,
            true,
            true
        );
    }

    /**
     * Attempt to match row {@code rowNumber}, once translated to characters according to {@code paletteMap}
     * against the regular expression described by {@code patternString}.
     *
     * @param image         image to take row from
     * @param rowNumber     image row number for comparison
     * @param paletteMap    {@link Map} of {@code Integer}s (denoting the or in ARGB format) to {@link String}s. It
     *                      is advised to only map colors to single characters. Colors with no corresponding entry in
     *                      the map are mapped to {@code #}.
     * @param patternString string representation of regular expression to match against. These are simply used to
     *                      construct a {@link Pattern} which is cached in case of re-use.
     * @param assertMatch   whether to raise an (informative) {@link AssertionFailedError} if no match is found.
     * @return {@link Matcher} produced by matching attempt
     */
    public static Matcher rowMatch(
        final BufferedImage image,
        final int rowNumber,
        final Map<Integer, String> paletteMap,
        final String patternString,
        final boolean assertMatch
    ) {
        return imageStripPatternMatchInner(
            image,
            rowNumber,
            null,
            paletteMap,
            null,
            patternString,
            false,
            true
        );
    }

    /**
     * Attempt to match row {@code rowNumber}, once translated to characters according to {@code paletteMapFn}
     * against the regular expression described by {@code patternString}.
     *
     * @param image         image to take row from
     * @param rowNumber     image row number for comparison
     * @param paletteMapFn  function mapping {@code Integer}s (denoting the color in ARGB format) to {@link String}s. It
     *                      is advised to only map colors to single characters.
     * @param patternString string representation of regular expression to match against. These are simply used to
     *                      construct a {@link Pattern} which is cached in case of re-use.
     * @param assertMatch   whether to raise an (informative) {@link AssertionFailedError} if no match is found.
     * @return {@link Matcher} produced by matching attempt
     */
    public static Matcher rowMatch(
        final BufferedImage image,
        final int rowNumber,
        final IntFunction<String> paletteMapFn,
        final String patternString,
        final boolean assertMatch
    ) {
        return imageStripPatternMatchInner(
            image,
            rowNumber,
            paletteMapFn,
            null,
            null,
            patternString,
            false,
            true
        );
    }

    /**
     * Attempt to match row {@code rowNumber}, once translated to characters according to {@code paletteMap}
     * against the regular expression {@code pattern}.
     *
     * @param image         image to take row from
     * @param rowNumber     image row number for comparison
     * @param paletteMap    {@link Map} of {@code Integer}s (denoting the color in ARGB format) to {@link String}s. It
     *                      is advised to only map colors to single characters. Colors with no corresponding entry in
     *                      the map are mapped to {@code #}.
     * @param pattern       regular expression to match against
     * @param assertMatch   whether to raise an (informative) {@link AssertionFailedError} if no match is found.
     * @return {@link Matcher} produced by matching attempt
     */
    public static Matcher rowMatch(
        final BufferedImage image,
        final int rowNumber,
        final Map<Integer, String> paletteMap,
        final Pattern pattern,
        final boolean assertMatch
    ) {
        return imageStripPatternMatchInner(
            image,
            rowNumber,
            null,
            paletteMap,
            pattern,
            null,
            false,
            true
        );
    }

    /**
     * Attempt to match row {@code rowNumber}, once translated to characters according to {@code paletteMapFn}
     * against the regular expression {@code pattern}.
     *
     * @param image         image to take row from
     * @param rowNumber     image row number for comparison
     * @param paletteMapFn  function mapping {@code Integer}s (denoting the color in ARGB format) to {@link String}s. It
     *                      is advised to only map colors to single characters.
     * @param pattern       regular expression to match against
     * @param assertMatch   whether to raise an (informative) {@link AssertionFailedError} if no match is found.
     * @return {@link Matcher} produced by matching attempt
     */
    public static Matcher rowMatch(
        final BufferedImage image,
        final int rowNumber,
        final IntFunction<String> paletteMapFn,
        final Pattern pattern,
        final boolean assertMatch
    ) {
        return imageStripPatternMatchInner(
            image,
            rowNumber,
            paletteMapFn,
            null,
            pattern,
            null,
            false,
            true
        );
    }
}
