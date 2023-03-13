// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;

/**
 * Unit tests of {@link UrlPatterns}.
 */
class UrlPatternsTest {

    private static final List<Class<? extends Enum<?>>> patterns = Arrays.asList(
            UrlPatterns.GeoJsonUrlPattern.class,
            UrlPatterns.GpxUrlPattern.class,
            UrlPatterns.NoteUrlPattern.class,
            UrlPatterns.OsmChangeUrlPattern.class,
            UrlPatterns.OsmUrlPattern.class);

    /**
     * Unit test of {@link UrlPatterns} enums.
     */
    @Test
    void testUrlPatternEnums() {
        patterns.forEach(TestUtils::superficialEnumCodeCoverage);
    }

    /**
     * Unit test of {@link UrlPatterns} syntax validity.
     */
    @Test
    void testUrlPatterns() {
        assertTrue(patterns.stream().flatMap(c -> Arrays.stream(c.getEnumConstants())).map(t -> ((UrlPattern) t).pattern())
                .map(Pattern::compile).findAny().isPresent());
    }
}
