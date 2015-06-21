// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools

class AlphanumComparatorTest extends GroovyTestCase {

    void testNumeric() {
        def lst = Arrays.asList("1", "20", "-1", "00999", "100")
        Collections.sort(lst, AlphanumComparator.getInstance())
        assert lst == Arrays.asList("-1", "1", "20", "100", "00999")
    }

    void testMixed() {
        def lst = Arrays.asList("b1", "b20", "a5", "a00999", "a100")
        Collections.sort(lst, AlphanumComparator.getInstance())
        assert lst == Arrays.asList("a5", "a100", "a00999", "b1", "b20")
    }

}
