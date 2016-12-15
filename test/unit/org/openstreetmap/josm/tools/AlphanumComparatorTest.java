// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

/**
 * Unit tests of {@link AlphanumComparator}.
 */
public class AlphanumComparatorTest {

    /**
     * Test numeric strings.
     */
    @Test
    public void testNumeric() {
        List<String> lst = Arrays.asList("1", "20", "-1", "00999", "100");
        Collections.sort(lst, AlphanumComparator.getInstance());
        assertEquals(Arrays.asList("-1", "1", "20", "100", "00999"), lst);
    }

    /**
     * Test mixed character strings.
     */
    @Test
    public void testMixed() {
        List<String> lst = Arrays.asList("b1", "b20", "a5", "a00999", "a100");
        Collections.sort(lst, AlphanumComparator.getInstance());
        assertEquals(Arrays.asList("a5", "a100", "a00999", "b1", "b20"), lst);
    }
}
