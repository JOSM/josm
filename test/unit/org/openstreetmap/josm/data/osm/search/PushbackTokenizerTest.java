// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.search;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.osm.search.PushbackTokenizer.Token;

/**
 * Unit tests for class {@link SearchCompiler}.
 */
class PushbackTokenizerTest {
    /**
     * Unit test of {@link Token} enum.
     */
    @Test
    void testEnumToken() {
        TestUtils.superficialEnumCodeCoverage(Token.class);
    }
}
