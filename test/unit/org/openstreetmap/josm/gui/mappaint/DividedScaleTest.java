// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.tools.Pair;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests of {@link DividedScale} class.
 */
class DividedScaleTest {

    /**
     * Test {@link DividedScale#get}
     */
    @Test
    void testGetEmpty() {
        DividedScale<String> scale = new DividedScale<>();
        assertThrows(IllegalArgumentException.class, () -> scale.get(0.));
        assertNull(scale.get(0.01));
        assertNull(scale.get(1.));
        assertNull(scale.get(4.));
        assertNull(scale.get(6.));
        assertNull(scale.get(8.));
        assertNull(scale.get(100.));
    }

    /**
     * Test {@link DividedScale#put} and {@link DividedScale#get}
     */
    @Test
    void testGetFoo() {
        DividedScale<String> scale = new DividedScale<String>()
                .put("foo", new Range(4., 8.));
        assertNull(scale.get(1.));
        assertNull(scale.get(4.));
        assertEquals("foo", scale.get(4.01));
        assertEquals("foo", scale.get(6.));
        assertEquals("foo", scale.get(8.));
        assertNull(scale.get(8.01));
        assertNull(scale.get(100.));
        assertEquals(scale, new DividedScale<>(scale));
    }

    /**
     * Test {@link DividedScale#put} and {@link DividedScale#getWithRange}
     */
    @Test
    void testGetWithRangeFoo() {
        DividedScale<String> scale = new DividedScale<String>()
                .put("foo", new Range(4., 8.));
        Pair<String, Range> pair = scale.getWithRange(6.);
        assertEquals("foo", pair.a);
        assertEquals(new Range(4., 8.), pair.b);
    }

    /**
     * Test {@link DividedScale#put} and {@link DividedScale#get}
     */
    @Test
    void testGetFooBar() {
        DividedScale<String> scale = new DividedScale<String>()
                .put("foo", new Range(4., 8.))
                .put("bar", new Range(2., 3.));
        assertNull(scale.get(2.));
        assertEquals("bar", scale.get(2.01));
        assertEquals("bar", scale.get(3.));
        assertNull(scale.get(3.01));
        assertNull(scale.get(4.));
        assertEquals("foo", scale.get(4.01));
        assertEquals("foo", scale.get(8.));
        assertNull(scale.get(8.01));
    }

    /**
     * Test {@link DividedScale#put} and {@link DividedScale#get}
     */
    @Test
    void testGetFooBarBaz() {
        DividedScale<String> scale = new DividedScale<String>()
                .put("foo", new Range(4., 8.))
                .put("bar", new Range(2., 3.))
                .put("baz", new Range(3., 4.));
        assertNull(scale.get(2.));
        assertEquals("bar", scale.get(2.01));
        assertEquals("bar", scale.get(3.));
        assertEquals("baz", scale.get(3.01));
        assertEquals("baz", scale.get(4.));
        assertEquals("foo", scale.get(4.01));
        assertEquals("foo", scale.get(8.));
        assertNull(scale.get(8.01));
    }

    /**
     * Test {@link DividedScale#put}
     */
    @Test
    void testPutSingleSubrange1() {
        DividedScale<String> scale = new DividedScale<String>()
                .put("foo", new Range(4., 8.));
        Exception ex = assertThrows(DividedScale.RangeViolatedError.class, () -> scale.put("bar", new Range(4., 9.)));
        assertEquals("the new range must be within a single subrange (1)", ex.getMessage());
    }

    /**
     * Test {@link DividedScale#put}
     */
    @Test
    void testPutSingleSubrangeNoData() {
        DividedScale<String> scale = new DividedScale<String>()
                .put("foo", new Range(4., 8.));
        Exception ex = assertThrows(DividedScale.RangeViolatedError.class, () -> scale.put("bar", new Range(4., 5.)));
        assertEquals("the new range must be within a subrange that has no data", ex.getMessage());
    }

    /**
     * Test {@link DividedScale#put}
     */
    @Test
    void testPutSingleSubrange2() {
        DividedScale<String> scale = new DividedScale<String>()
                .put("foo", new Range(4., 8.));
        Exception ex = assertThrows(DividedScale.RangeViolatedError.class, () -> scale.put("bar", new Range(2., 5.)));
        assertEquals("the new range must be within a single subrange (2)", ex.getMessage());
    }
}
