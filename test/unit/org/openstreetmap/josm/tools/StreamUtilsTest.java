// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.stream.Collectors;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.trajano.commons.testing.UtilityClassTestUtil;

/**
 * Unit tests of {@link StreamUtils} class.
 */
public class StreamUtilsTest {

    /**
     * Setup rule.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Tests that {@code StreamUtils} satisfies utility class criteria.
     * @throws ReflectiveOperationException if an error occurs
     */
    @Test
    public void testUtilityClass() throws ReflectiveOperationException {
        UtilityClassTestUtil.assertUtilityClassWellDefined(StreamUtils.class);
    }

    /**
     * Tests {@link StreamUtils#reversedStream(java.util.List)}
     */
    @Test
    public void testReverseStream() {
        assertEquals("baz/bar/foo",
                StreamUtils.reversedStream(Arrays.asList("foo", "bar", "baz")).collect(Collectors.joining("/")));
    }
}
