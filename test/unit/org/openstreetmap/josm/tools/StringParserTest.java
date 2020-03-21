// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import net.trajano.commons.testing.UtilityClassTestUtil;
import org.junit.Test;

/**
 * Unit tests of {@link StringParser} class.
 */
public class StringParserTest {

    /**
     * Tests that {@code Utils} satisfies utility class criteria.
     *
     * @throws ReflectiveOperationException if an error occurs
     */
    @Test
    public void testUtilityClass() throws ReflectiveOperationException {
        UtilityClassTestUtil.assertUtilityClassWellDefined(Utils.class);
    }

    /**
     * Test of {@link StringParser#parse}
     */
    @Test
    public void testParse() {
        assertThat(StringParser.DEFAULT.parse(char.class, "josm"), is('j'));
        assertThat(StringParser.DEFAULT.parse(short.class, "123"), is((short) 123));
        assertThat(StringParser.DEFAULT.parse(int.class, "123456"), is(123456));
        assertThat(StringParser.DEFAULT.parse(long.class, "1234567890123"), is(1234567890123L));
        assertThat(StringParser.DEFAULT.tryParse(long.class, "123.456"), is(Optional.empty()));
        assertThat(StringParser.DEFAULT.tryParse(long.class, "1234567890123"), is(Optional.of(1234567890123L)));
    }

    /**
     * Tests that {@link StringParser#DEFAULT} is immutable.
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testDefaultImmutable() {
        StringParser.DEFAULT.registerParser(String.class, String::valueOf);
    }

    /**
     * Tests that {@link StringParser#StringParser(StringParser)} creates a new map.
     */
    @Test
    public void testCopyConstructor() {
        final StringParser parser = new StringParser(StringParser.DEFAULT).registerParser(boolean.class, "JOSM"::equals);
        assertTrue(StringParser.DEFAULT.parse(boolean.class, "true"));
        assertFalse(StringParser.DEFAULT.parse(boolean.class, "JOSM"));
        assertFalse(parser.parse(boolean.class, "true"));
        assertTrue(parser.parse(boolean.class, "JOSM"));
        assertThat(parser.parse(int.class, "123"), is(123));
    }
}
